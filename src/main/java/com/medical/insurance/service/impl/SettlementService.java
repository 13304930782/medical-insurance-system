package com.medical.insurance.service.impl;

import com.medical.insurance.dao.SettlementMapper;
import com.medical.insurance.exception.ReimbursementBusinessException;
import com.medical.insurance.model.SettlementRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.dao.ApprovalMapper;
import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final SettlementMapper mapper;
    private final ApprovalMapper approvalMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public SettlementService(SettlementMapper mapper,ApprovalMapper approvalMapper,AuthService authService,SystemMapper systemMapper){
        this.mapper=mapper;this.approvalMapper=approvalMapper;this.authService=authService;this.systemMapper=systemMapper;
    }

    public Map<String,Object> preview(String number){return calculate(number);}

    @Transactional
    public Map<String,Object> settle(String number,HttpServletRequest request){
        String flag=mapper.lockVisit(number);
        if(flag==null)throw new ReimbursementBusinessException("未找到该就诊资料");
        if(isSettled(flag))throw new ReimbursementBusinessException("该就诊资料已经正式结算，不能重复结算");
        lockAnnualForVisit(number);
        Map<String,Object> result=calculate(number);
        long operatorId=authService.currentUserId(request);
        SettlementRecord record=toRecord(result,1,null,operatorId,"SETTLED",null);
        mapper.insertSettlement(record);
        saveDetails(record.getSettlementId(),result,BigDecimal.ONE);
        String personId=text(result.get("personId"));
        int year=((Number)result.get("year")).intValue();
        mapper.addAnnualExpense(personId,year,money(result.get("totalFee")),money(result.get("fundFee")),money(result.get("personalFee")));
        mapper.updateVisitSettlementFlag(number,"已结算");
        record(request,"SETTLE",record.getSettlementNo(),"正式结算住院号："+number);
        return settlement(record.getSettlementId());
    }

    public List<Map<String,Object>> settlements(String keyword,Integer year){return mapper.findSettlements(normalize(keyword),year);}

    public Map<String,Object> settlement(Long id){
        Map<String,Object> base=mapper.findSettlement(id);
        if(base==null)throw new ReimbursementBusinessException("未找到该结算记录");
        Map<String,Object> result=new LinkedHashMap<>(base);
        result.put("breakdowns",mapper.breakdowns(id));
        result.put("items",mapper.itemResults(id));
        Map<String,Object> visit=mapper.calculationVisit(text(base.get("hospitalizationNumber")));
         if(visit!=null){result.put("visit",visit);int year=yearOf(visit);result.put("year",year);result.put("annualExpense",mapper.annualExpense(text(visit.get("personId")),year));}
        return result;
    }

    @Transactional
    public Map<String,Object> cancel(Long id,String reason,HttpServletRequest request){
        String normalized=normalize(reason);
        if(normalized==null)throw new ReimbursementBusinessException("取消原因不能为空");
        Map<String,Object> original=mapper.lockSettlement(id);
        if(original==null)throw new ReimbursementBusinessException("未找到该结算记录");
        if(((Number)original.get("transactionType")).intValue()!=1)throw new ReimbursementBusinessException("负交易记录不能再次取消");
        if("CANCELLED".equalsIgnoreCase(text(original.get("settlementStatus")))||mapper.cancellationCount(id)>0)throw new ReimbursementBusinessException("该结算已经取消，不能重复操作");
        String number=text(original.get("hospitalizationNumber"));
        mapper.lockVisit(number);
        Map<String,Object> visit=mapper.calculationVisit(number);
        if(visit==null)throw new ReimbursementBusinessException("原结算对应的就诊资料不存在");
        lockAnnual(text(visit.get("personId")),yearOf(visit));
        long operatorId=authService.currentUserId(request);
        SettlementRecord negative=new SettlementRecord();
        negative.setSettlementNo(newSettlementNo("CX"));negative.setHospitalizationNumber(number);negative.setTransactionType(0);negative.setOriginalSettlementId(id);
        negative.setTotalFee(money(original.get("totalFee")).negate());negative.setEligibleFee(money(original.get("eligibleFee")).negate());negative.setOverLimitSelfFee(money(original.get("overLimitSelfFee")).negate());
        negative.setDeductibleSelfFee(money(original.get("deductibleSelfFee")).negate());negative.setSegmentSelfFee(money(original.get("segmentSelfFee")).negate());negative.setPersonalFee(money(original.get("personalFee")).negate());negative.setFundFee(money(original.get("fundFee")).negate());
        negative.setSettlementStatus("CANCELLED");negative.setCancelReason(normalized);negative.setOperatorId(operatorId);negative.setSettledAt(LocalDateTime.now());
        mapper.insertSettlement(negative);
        for(Map<String,Object> row:mapper.breakdowns(id))mapper.insertBreakdown(negative.getSettlementId(),text(row.get("feeType")),money(row.get("amount")).negate());
         for(Map<String,Object> source:mapper.itemResults(id)){Map<String,Object> row=new LinkedHashMap<>(source);row.put("totalFee",money(source.get("totalFee")).negate());row.put("eligibleFee",money(source.get("eligibleFee")).negate());row.put("selfFee",money(source.get("selfFee")).negate());row.put("calculationNote","取消冲回："+text(source.get("calculationNote")));mapper.insertItemResult(negative.getSettlementId(),row);}
        int year=yearOf(visit);
        if(mapper.subtractAnnualExpense(text(visit.get("personId")),year,money(original.get("totalFee")),money(original.get("fundFee")),money(original.get("personalFee")))!=1)throw new ReimbursementBusinessException("年度累计记录不存在，取消结算已终止");
        mapper.cancelOriginal(id,normalized);mapper.updateVisitSettlementFlag(number,"未结算");
        record(request,"CANCEL",negative.getSettlementNo(),"取消原结算："+text(original.get("settlementNo"))+"，原因："+normalized);
        return settlement(negative.getSettlementId());
    }

    private Map<String,Object> calculate(String number){
        Map<String,Object> visit=mapper.calculationVisit(number);
        if(visit==null)throw new ReimbursementBusinessException("未找到该就诊资料");
        required(visit,"personId","就诊人员不存在");required(visit,"medicalPersonnel","人员医疗类别未维护");required(visit,"designatedNumber","就诊医疗机构未维护");required(visit,"institutionName","就诊医疗机构不存在");required(visit,"hospitalGrade","医院等级未维护");required(visit,"medicalCategory","医疗类别未维护");required(visit,"diseaseCode","病种未维护");required(visit,"diseaseName","病种不存在");
        String diseaseFlag=text(visit.get("diseaseReimbursementStandard"));
        if(diseaseFlag.isEmpty())throw new ReimbursementBusinessException("病种报销标准未维护");
        if(diseaseFlag.contains("不报销")||diseaseFlag.contains("不可")||diseaseFlag.contains("无效"))throw new ReimbursementBusinessException("该病种不符合基本医疗保险报销资格");
        LocalDate visitDate=dateOf(visit.get("admissionDate"));
        if(!active(text(visit.get("institutionValidFlag")),visit.get("institutionValidFrom"),visit.get("institutionValidTo"),visitDate))throw new ReimbursementBusinessException("就诊医疗机构无效或不在有效期内");
        String registered=text(visit.get("registeredInstitution"));
        if(registered.isEmpty())throw new ReimbursementBusinessException("人员未登记定点医疗机构");
        if(!registered.equals(text(visit.get("designatedNumber")))&&approvalMapper.hasActiveInstitutionApproval(text(visit.get("personId")),text(visit.get("designatedNumber")),visitDate)==0)throw new ReimbursementBusinessException("人员在该医疗机构就诊没有有效审批");
        List<Map<String,Object>> sourceItems=mapper.calculationItems(number);
        if(sourceItems.isEmpty())throw new ReimbursementBusinessException("该就诊资料没有处方明细，不能结算");

        List<Map<String,Object>> itemResults=new ArrayList<>();Map<String,BigDecimal> breakdown=new LinkedHashMap<>();
        add(breakdown,"OVER_LIMIT_SELF",ZERO);add(breakdown,"CLASS_SELF",ZERO);add(breakdown,"HOSPITAL_SELF",ZERO);add(breakdown,"SPECIAL_SELF",ZERO);add(breakdown,"NON_CATALOG_SELF",ZERO);add(breakdown,"INVALID_ITEM_SELF",ZERO);add(breakdown,"DEDUCTIBLE_SELF",ZERO);add(breakdown,"SEGMENT_SELF",ZERO);add(breakdown,"CAP_SELF",ZERO);
        BigDecimal total=ZERO,eligible=ZERO;
         for(Map<String,Object> item:sourceItems){
            BigDecimal itemTotal=money(item.get("totalFee"));total=total.add(itemTotal);BigDecimal itemEligible=ZERO;String note;
             if(!"CATALOG".equalsIgnoreCase(text(item.get("sourceType")))||"MANUAL".equalsIgnoreCase(text(item.get("catalogType")))){note="手工录入/目录外项目，全额自费";add(breakdown,"NON_CATALOG_SELF",itemTotal);}
             else if(!catalogExists(item)){note="目录项目不存在或已被删除，全额自费";add(breakdown,"INVALID_ITEM_SELF",itemTotal);}
             else if(!itemValid(item,visitDate)){note="目录项目无效或不在有效期，全额自费";add(breakdown,"INVALID_ITEM_SELF",itemTotal);}
             else if(requiresExpenseLevel(item)&&text(item.get("expenseLevel")).isEmpty()){note="目录项目未维护收费等级，全额自费";add(breakdown,"INVALID_ITEM_SELF",itemTotal);}
             else if(!hospitalAllowed(text(item.get("itemHospitalLevel")),text(visit.get("hospitalGrade")))){note="当前医院等级不符合项目限制，全额自费";add(breakdown,"HOSPITAL_SELF",itemTotal);}
             else if(needsApproval(item)&&approvalMapper.hasActiveSpecialApproval(text(visit.get("personId")),text(item.get("catalogType")),text(item.get("projectCoding")),visitDate)==0){note="缺少有效特检特治审批，全额自费";add(breakdown,"SPECIAL_SELF",itemTotal);}
            else{
                BigDecimal allowed=itemTotal;BigDecimal max=money(item.get("maxPrice"));BigDecimal quantity=decimal(item.get("quantity"));
                 if(max.compareTo(BigDecimal.ZERO)>0){BigDecimal maximum=max.multiply(quantity).setScale(2,RoundingMode.HALF_UP);if(allowed.compareTo(maximum)>0){add(breakdown,"OVER_LIMIT_SELF",allowed.subtract(maximum));allowed=maximum;}}
                BigDecimal classRate=classRate(text(item.get("expenseLevel")));itemEligible=allowed.multiply(classRate).setScale(2,RoundingMode.HALF_UP);BigDecimal classSelf=allowed.subtract(itemEligible);add(breakdown,"CLASS_SELF",classSelf);
                note=classNote(text(item.get("expenseLevel")),itemTotal.subtract(allowed),classSelf);
            }
            eligible=eligible.add(itemEligible);Map<String,Object> row=new LinkedHashMap<>();row.put("chargeableItemsCategory",item.get("chargeableItemsCategory"));row.put("projectCoding",item.get("projectCoding"));row.put("projectName",item.get("projectName"));row.put("totalFee",itemTotal);row.put("eligibleFee",itemEligible);row.put("selfFee",itemTotal.subtract(itemEligible));row.put("calculationNote",note);itemResults.add(row);
        }
        total=scale(total);eligible=scale(eligible);
        BigDecimal minimum=mapper.minimumStandard(text(visit.get("medicalCategory")),text(visit.get("medicalPersonnel")),text(visit.get("hospitalGrade")));
        if(minimum==null)throw new ReimbursementBusinessException("未维护当前医疗类别、人员类别和医院等级对应的起付标准");
        BigDecimal deductible=eligible.min(minimum).setScale(2,RoundingMode.HALF_UP);breakdown.put("DEDUCTIBLE_SELF",deductible);
        BigDecimal segmentBase=eligible.subtract(deductible);BigDecimal segmentFund=ZERO,covered=ZERO;
         if(segmentBase.compareTo(BigDecimal.ZERO)>0){
            List<Map<String,Object>> segments=mapper.segmentRatios(text(visit.get("medicalCategory")),text(visit.get("medicalPersonnel")),text(visit.get("hospitalGrade")));
            if(segments.isEmpty())throw new ReimbursementBusinessException("未维护当前医疗类别、人员类别和医院等级对应的分段报销比例");
             for(Map<String,Object> segment:segments){BigDecimal low=money(segment.get("minimumAmount")),high=money(segment.get("maximumAmount"));BigDecimal left=deductible.max(low),right=eligible.min(high);if(right.compareTo(left)>0){BigDecimal width=right.subtract(left);covered=covered.add(width);segmentFund=segmentFund.add(width.multiply(decimal(segment.get("reimbursementProportion"))));}}
            if(covered.setScale(2,RoundingMode.HALF_UP).compareTo(segmentBase.setScale(2,RoundingMode.HALF_UP))!=0)throw new ReimbursementBusinessException("分段比例参数未完整覆盖起付线以上的实际费用区间");
        }
        segmentFund=scale(segmentFund);BigDecimal segmentSelf=scale(segmentBase.subtract(segmentFund));breakdown.put("SEGMENT_SELF",segmentSelf);
        BigDecimal cap=mapper.cappingLine(text(visit.get("medicalPersonnel")));if(cap==null)throw new ReimbursementBusinessException("未维护当前医疗人员类别的封顶线");
        int year=yearOf(visit);Map<String,Object> annual=mapper.annualExpense(text(visit.get("personId")),year);BigDecimal annualFund=annual==null?ZERO:money(annual.get("medicareExpenses"));BigDecimal available=cap.subtract(annualFund).max(BigDecimal.ZERO);BigDecimal fund=segmentFund.min(available).setScale(2,RoundingMode.HALF_UP);BigDecimal capSelf=segmentFund.subtract(fund).setScale(2,RoundingMode.HALF_UP);breakdown.put("CAP_SELF",capSelf);
        BigDecimal personal=total.subtract(fund).setScale(2,RoundingMode.HALF_UP);
        Map<String,Object> result=new LinkedHashMap<>();result.putAll(visit);result.put("year",year);result.put("totalFee",total);result.put("eligibleFee",eligible);result.put("overLimitSelfFee",breakdown.get("OVER_LIMIT_SELF"));result.put("deductibleSelfFee",deductible);result.put("segmentSelfFee",segmentSelf);result.put("capSelfFee",capSelf);result.put("personalFee",personal);result.put("fundFee",fund);result.put("cappingLine",cap);result.put("annualFundBefore",annualFund);result.put("annualFundAfter",annualFund.add(fund));result.put("breakdowns",breakdownRows(breakdown));result.put("items",itemResults);return result;
    }

    private SettlementRecord toRecord(Map<String,Object> result,int type,Long original,long operator,String status,String reason){SettlementRecord r=new SettlementRecord();r.setSettlementNo(newSettlementNo("JS"));r.setHospitalizationNumber(text(result.get("hospitalizationNumber")));r.setTransactionType(type);r.setOriginalSettlementId(original);r.setTotalFee(money(result.get("totalFee")));r.setEligibleFee(money(result.get("eligibleFee")));r.setOverLimitSelfFee(money(result.get("overLimitSelfFee")));r.setDeductibleSelfFee(money(result.get("deductibleSelfFee")));r.setSegmentSelfFee(money(result.get("segmentSelfFee")));r.setPersonalFee(money(result.get("personalFee")));r.setFundFee(money(result.get("fundFee")));r.setSettlementStatus(status);r.setCancelReason(reason);r.setOperatorId(operator);r.setSettledAt(LocalDateTime.now());return r;}
    @SuppressWarnings("unchecked") private void saveDetails(Long id,Map<String,Object> result,BigDecimal multiplier){for(Map<String,Object> row:(List<Map<String,Object>>)result.get("breakdowns"))mapper.insertBreakdown(id,text(row.get("feeType")),money(row.get("amount")).multiply(multiplier));for(Map<String,Object> row:(List<Map<String,Object>>)result.get("items"))mapper.insertItemResult(id,row);}
    private List<Map<String,Object>> breakdownRows(Map<String,BigDecimal> values){List<Map<String,Object>> rows=new ArrayList<>();for(Map.Entry<String,BigDecimal> e:values.entrySet()){Map<String,Object> row=new LinkedHashMap<>();row.put("feeType",e.getKey());row.put("amount",scale(e.getValue()));rows.add(row);}return rows;}
    private void add(Map<String,BigDecimal> values,String key,BigDecimal amount){values.put(key,values.getOrDefault(key,ZERO).add(amount));}
    private boolean catalogExists(Map<String,Object> item){return decimal(item.get("catalogExists")).compareTo(BigDecimal.ONE)==0;}
    private boolean requiresExpenseLevel(Map<String,Object> item){String type=text(item.get("catalogType"));return "MEDICINE".equalsIgnoreCase(type)||"DIAGNOSIS".equalsIgnoreCase(type);}
    private boolean itemValid(Map<String,Object> item,LocalDate visitDate){return active(text(item.get("validFlag")),item.get("validFrom"),item.get("validTo"),visitDate);}
    private boolean active(String flag,Object fromValue,Object toValue,LocalDate date){if(flag.isEmpty()||flag.contains("无效")||"0".equals(flag))return false;LocalDate from=dateOfNullable(fromValue),to=dateOfNullable(toValue);return (from==null||!date.isBefore(from))&&(to==null||!date.isAfter(to));}
    private boolean needsApproval(Map<String,Object> item){String value=text(item.get("approvalMark"))+text(item.get("specialMark"));return (value.contains("需要")&&!value.contains("不需要"))||value.contains("是")||value.contains("特检")||"1".equals(value);}
    private boolean hospitalAllowed(String itemLevel,String actual){if(itemLevel.isEmpty()||itemLevel.contains("所有"))return true;int required=level(itemLevel),current=level(actual);return required>0&&current>0?current<=required:itemLevel.equals(actual);}
    private int level(String value){if(value.contains("一级"))return 1;if(value.contains("二级"))return 2;if(value.contains("三级"))return 3;return 0;}
    private BigDecimal classRate(String level){if(level.contains("丙"))return BigDecimal.ZERO;if(level.contains("乙"))return new BigDecimal("0.50");return BigDecimal.ONE;}
    private String classNote(String level,BigDecimal over,BigDecimal classSelf){String category=level.isEmpty()?"未标等级（按甲类）":level;return category+"；超最高限价自费"+scale(over)+"元；类别自费"+scale(classSelf)+"元";}
    private boolean isSettled(String flag){return "已结算".equals(flag)||"SETTLED".equalsIgnoreCase(flag);}
    private String newSettlementNo(String prefix){return prefix+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+UUID.randomUUID().toString().replace("-","").substring(0,6).toUpperCase();}
    private int yearOf(Map<String,Object> visit){Object date=visit.get("dischargeDate")!=null?visit.get("dischargeDate"):visit.get("admissionDate");return dateOf(date).getYear();}
    private void lockAnnualForVisit(String number){Map<String,Object> visit=mapper.calculationVisit(number);if(visit==null)throw new ReimbursementBusinessException("未找到该就诊资料");lockAnnual(text(visit.get("personId")),yearOf(visit));}
    private void lockAnnual(String personId,int year){mapper.ensureAnnualExpense(personId,year);if(mapper.lockAnnualExpense(personId,year)==null)throw new ReimbursementBusinessException("无法锁定年度累计记录");}
    private LocalDate dateOf(Object value){LocalDate date=dateOfNullable(value);if(date==null)throw new ReimbursementBusinessException("就诊日期不能为空");return date;}
    private LocalDate dateOfNullable(Object value){if(value==null)return null;if(value instanceof LocalDate)return (LocalDate)value;if(value instanceof LocalDateTime)return ((LocalDateTime)value).toLocalDate();if(value instanceof java.sql.Timestamp)return ((java.sql.Timestamp)value).toLocalDateTime().toLocalDate();if(value instanceof java.sql.Date)return ((java.sql.Date)value).toLocalDate();if(value instanceof Date)return ((Date)value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();String s=value.toString().replace(' ','T');return s.length()==10?LocalDate.parse(s):LocalDateTime.parse(s).toLocalDate();}
    private BigDecimal decimal(Object value){if(value==null)return BigDecimal.ZERO;return value instanceof BigDecimal?(BigDecimal)value:new BigDecimal(value.toString());}
    private BigDecimal money(Object value){return scale(decimal(value));}private BigDecimal scale(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    private void required(Map<String,Object> map,String key,String message){if(text(map.get(key)).isEmpty())throw new ReimbursementBusinessException(message);}
    private String text(Object value){return value==null?"":String.valueOf(value).trim();}private String normalize(String value){String s=text(value);return s.isEmpty()?null:s;}
    private void record(HttpServletRequest request,String type,String no,String content){systemMapper.recordOperation(authService.currentUserId(request),"医疗待遇结算",type,no,content,"SUCCESS",request.getRemoteAddr());}
}
