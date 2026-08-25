package com.medical.insurance.service.impl;

import com.medical.insurance.dao.ReimbursementMapper;
import com.medical.insurance.exception.ReimbursementBusinessException;
import com.medical.insurance.model.PrescriptionForm;
import com.medical.insurance.model.VisitForm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import com.medical.insurance.dao.DictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReimbursementService {
    private final ReimbursementMapper mapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;
    private final DictionaryMapper dictionaryMapper;

    public ReimbursementService(ReimbursementMapper mapper,AuthService authService,SystemMapper systemMapper,DictionaryMapper dictionaryMapper){this.mapper=mapper;this.authService=authService;this.systemMapper=systemMapper;this.dictionaryMapper=dictionaryMapper;}

    public List<Map<String,Object>> visits(String keyword){return mapper.findVisits(normalize(keyword));}
    public Map<String,Object> visit(String number){Map<String,Object> visit=mapper.findVisit(number);if(visit==null)throw new ReimbursementBusinessException("未找到该就诊资料");Map<String,Object> result=new LinkedHashMap<>(visit);result.put("prescriptions",mapper.findPrescriptions(number));return result;}
    public Map<String,Object> reusableOutpatientVisit(String personId){
        String id=required(personId,"个人编号不能为空");
        if(mapper.personExists(id)==0)throw new ReimbursementBusinessException("个人编号不存在");
        Map<String,Object> candidate=mapper.findReusableOutpatientVisit(id);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("hasCandidate",candidate!=null);
        result.put("candidate",candidate);
        return result;
    }
    public List<Map<String,Object>> prescriptions(String number){if(mapper.visitExists(number)==0)throw new ReimbursementBusinessException("未找到该就诊资料");return mapper.findPrescriptions(number);}

    @Transactional
    public Map<String,Object> createVisit(VisitForm form,HttpServletRequest request){
        if(form==null)throw new ReimbursementBusinessException("就诊资料不能为空");
        if(isOutpatient(form.getMedicalCategory())&&blank(form.getHospitalizationNumber()))form.setHospitalizationNumber(nextOutpatientNumber());
        validateVisit(form,true);
        if(mapper.visitExists(form.getHospitalizationNumber())>0)throw new ReimbursementBusinessException("住院号（门诊号）已存在");
        mapper.insertVisit(form);
        record(request,"CREATE",form.getHospitalizationNumber(),"新增人员就诊资料");
        return mapper.findVisit(form.getHospitalizationNumber());
    }
    @Transactional
    public void updateVisit(String number,VisitForm form,HttpServletRequest request){Map<String,Object> existing=visit(number);if(isSettled(existing.get("settlementFlag")))throw new ReimbursementBusinessException("已结算的就诊资料不能修改");form.setHospitalizationNumber(number);form.setSettlementFlag(String.valueOf(existing.get("settlementFlag")));validateVisit(form,false);if(mapper.updateVisit(form)==0)throw new ReimbursementBusinessException("未找到该就诊资料");record(request,"UPDATE",number,"修改人员就诊资料");}
    @Transactional
    public void deleteVisit(String number,HttpServletRequest request){Map<String,Object> existing=visit(number);if(isSettled(existing.get("settlementFlag")))throw new ReimbursementBusinessException("已结算的就诊资料不能删除");if(mapper.settlementHistoryCount(number)>0)throw new ReimbursementBusinessException("该就诊资料存在结算或取消历史，不能删除");mapper.deleteVisitExtensions(number);mapper.deleteVisitPrescriptions(number);mapper.deleteVisit(number);record(request,"DELETE",number,"删除人员就诊资料及处方明细");}

    @Transactional
    public PrescriptionForm createPrescription(String number,PrescriptionForm form,HttpServletRequest request){ensureVisitEditable(number);form.setHospitalizationNumber(number);validatePrescription(form,true);if(mapper.prescriptionExists(form)>0)throw new ReimbursementBusinessException("相同收费类别和项目编码的处方明细已存在");mapper.insertPrescription(form);mapper.upsertPrescriptionExtension(form);record(request,"CREATE",number,"新增处方明细："+form.getProjectName());return form;}
    @Transactional
    public void updatePrescription(String number,PrescriptionForm form,HttpServletRequest request){ensureVisitEditable(number);form.setHospitalizationNumber(number);validatePrescription(form,false);if(mapper.updatePrescription(form)==0)throw new ReimbursementBusinessException("未找到该处方明细");mapper.upsertPrescriptionExtension(form);record(request,"UPDATE",number,"修改处方明细："+form.getProjectName());}
    @Transactional
    public void deletePrescription(String number,PrescriptionForm form,HttpServletRequest request){ensureVisitEditable(number);form.setHospitalizationNumber(number);form.setChargeableItemsCategory(required(form.getChargeableItemsCategory(),"收费项目类别不能为空"));form.setProjectCoding(required(form.getProjectCoding(),"项目编码不能为空"));mapper.deletePrescriptionExtension(form);if(mapper.deletePrescription(form)==0)throw new ReimbursementBusinessException("未找到该处方明细");record(request,"DELETE",number,"删除处方明细："+form.getProjectCoding());}

    private void validateVisit(VisitForm form,boolean creating){form.setHospitalizationNumber(required(form.getHospitalizationNumber(),"住院号（门诊号）不能为空"));form.setPersonId(required(form.getPersonId(),"个人编号不能为空"));form.setDesignatedNumber(required(form.getDesignatedNumber(),"定点医疗机构不能为空"));form.setMedicalCategory(required(form.getMedicalCategory(),"医疗类别不能为空"));form.setDiseaseCode(required(form.getDiseaseCode(),"病种编码不能为空"));form.setHospitalGrade(required(form.getHospitalGrade(),"医院等级不能为空"));form.setAdmissionCode(trim(form.getAdmissionCode()));form.setDiagnosedName(trim(form.getDiagnosedName()));form.setDischargeReason(trim(form.getDischargeReason()));if(form.getAdmissionDate()==null)throw new ReimbursementBusinessException("入院日期不能为空");if(form.getDischargeDate()!=null&&form.getDischargeDate().isBefore(form.getAdmissionDate()))throw new ReimbursementBusinessException("出院日期不能早于入院日期");if(mapper.personExists(form.getPersonId())==0)throw new ReimbursementBusinessException("个人编号不存在");dictionary("医疗类别",form.getMedicalCategory());Map<String,Object> institution=mapper.institution(form.getDesignatedNumber());if(institution==null)throw new ReimbursementBusinessException("定点医疗机构不存在");if(!active(institution,form.getAdmissionDate().toLocalDate()))throw new ReimbursementBusinessException("定点医疗机构无效或不在入院日期有效期内");String institutionLevel=string(institution.get("hospitalLevel"));if(!blank(institutionLevel)&&!institutionLevel.equals(form.getHospitalGrade()))throw new ReimbursementBusinessException("医院等级与所选定点医疗机构不一致，应为"+institutionLevel);if(!blank(institutionLevel))form.setHospitalGrade(institutionLevel);dictionary("医院等级",form.getHospitalGrade());if(mapper.diseaseExists(form.getDiseaseCode())==0)throw new ReimbursementBusinessException("病种编码不存在");if(creating)form.setSettlementFlag("未结算");else if(blank(form.getSettlementFlag()))throw new ReimbursementBusinessException("结算状态缺失，不能修改");}
    private String nextOutpatientNumber(){
        for(int attempt=0;attempt<1000;attempt++){
            if(mapper.advanceOutpatientSequence()!=1)throw new ReimbursementBusinessException("门诊号序列不存在，请执行数据库迁移");
            String number=String.format("MZ%08d",mapper.lastInsertId());
            if(mapper.visitExists(number)==0)return number;
        }
        throw new ReimbursementBusinessException("无法生成可用门诊号，请检查门诊号序列");
    }
    private boolean isOutpatient(String medicalCategory){return !blank(medicalCategory)&&medicalCategory.trim().contains("门诊");}
    private void validatePrescription(PrescriptionForm form,boolean creating){form.setSourceType(blank(form.getSourceType())?"CATALOG":form.getSourceType().trim().toUpperCase());form.setCatalogType(blank(form.getCatalogType())?"MANUAL":form.getCatalogType().trim().toUpperCase());if(!"CATALOG".equals(form.getSourceType())&&!"MANUAL".equals(form.getSourceType()))throw new ReimbursementBusinessException("项目来源只能是目录选择或手工录入");Map<String,Object> catalog=null;if("CATALOG".equals(form.getSourceType())){form.setProjectCoding(required(form.getProjectCoding(),"目录项目编码不能为空"));if("MEDICINE".equals(form.getCatalogType()))catalog=mapper.medicine(form.getProjectCoding());else if("DIAGNOSIS".equals(form.getCatalogType()))catalog=mapper.diagnosis(form.getProjectCoding());else if("FACILITY".equals(form.getCatalogType()))catalog=mapper.facility(form.getProjectCoding());else throw new ReimbursementBusinessException("目录类别不正确");if(catalog==null)throw new ReimbursementBusinessException("目录项目编码不存在");form.setProjectName(String.valueOf(catalog.get("projectName")));String catalogCategory=catalog.get("chargeableItemsCategory")==null?null:String.valueOf(catalog.get("chargeableItemsCategory"));if(!blank(form.getChargeableItemsCategory())&&!blank(catalogCategory)&&!catalogCategory.equals(form.getChargeableItemsCategory().trim()))throw new ReimbursementBusinessException("收费项目类别与所选目录项目不一致，应为"+catalogCategory);if(!blank(catalogCategory))form.setChargeableItemsCategory(catalogCategory);}else{form.setCatalogType("MANUAL");if(blank(form.getProjectCoding())&&creating)form.setProjectCoding("MANUAL-"+UUID.randomUUID().toString().replace("-","").substring(0,25));form.setProjectCoding(required(form.getProjectCoding(),"项目编码不能为空"));form.setProjectName(required(form.getProjectName(),"项目名称不能为空"));}form.setChargeableItemsCategory(required(form.getChargeableItemsCategory(),"收费项目类别不能为空"));nonNegative(form.getUnitPrice(),"单价");if(form.getQuantity()==null||form.getQuantity().compareTo(BigDecimal.ZERO)<=0)throw new ReimbursementBusinessException("数量必须大于0");form.setUnitPrice(form.getUnitPrice().setScale(2,RoundingMode.HALF_UP));form.setQuantity(form.getQuantity().setScale(4,RoundingMode.HALF_UP));form.setAmount(form.getUnitPrice().multiply(form.getQuantity()).setScale(2,RoundingMode.HALF_UP));}
    private void ensureVisitEditable(String number){Map<String,Object> visit=visit(number);if(isSettled(visit.get("settlementFlag")))throw new ReimbursementBusinessException("已结算的就诊资料不能修改处方");}
    private void dictionary(String category,String value){if(dictionaryMapper.contains(category,value)==0)throw new ReimbursementBusinessException(category+"不在参数字典中："+value);}
    private boolean active(Map<String,Object> row,LocalDate date){String flag=string(row.get("validFlag"));if(flag.isEmpty()||flag.contains("无效")||"0".equals(flag))return false;LocalDate from=date(row.get("validFrom")),to=date(row.get("validTo"));return (from==null||!date.isBefore(from))&&(to==null||!date.isAfter(to));}
    private LocalDate date(Object value){if(value==null)return null;if(value instanceof LocalDate)return (LocalDate)value;if(value instanceof LocalDateTime)return ((LocalDateTime)value).toLocalDate();if(value instanceof java.sql.Timestamp)return ((java.sql.Timestamp)value).toLocalDateTime().toLocalDate();if(value instanceof java.sql.Date)return ((java.sql.Date)value).toLocalDate();if(value instanceof Date)return ((Date)value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();return LocalDateTime.parse(value.toString().replace(' ','T')).toLocalDate();}
    private String string(Object value){return value==null?"":String.valueOf(value).trim();}
    private boolean isSettled(Object flag){return flag!=null&&("已结算".equals(String.valueOf(flag))||"SETTLED".equalsIgnoreCase(String.valueOf(flag)));}
    private void nonNegative(BigDecimal value,String label){if(value==null)throw new ReimbursementBusinessException(label+"不能为空");if(value.compareTo(BigDecimal.ZERO)<0)throw new ReimbursementBusinessException(label+"不能小于0");}
    private void record(HttpServletRequest request,String type,String no,String content){systemMapper.recordOperation(authService.currentUserId(request),"中心报销资料维护",type,no,content,"SUCCESS",request.getRemoteAddr());}
    private String normalize(String value){return blank(value)?null:value.trim();}private String required(String value,String message){if(blank(value))throw new ReimbursementBusinessException(message);return value.trim();}private String trim(String value){return value==null?null:value.trim();}private boolean blank(String value){return value==null||value.trim().isEmpty();}
}
