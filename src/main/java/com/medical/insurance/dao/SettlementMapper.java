package com.medical.insurance.dao;

import com.medical.insurance.model.SettlementRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SettlementMapper {
    @Select("SELECT v.hospitalization_number AS hospitalizationNumber,v.person_ID AS personId,p.name AS personName,p.medical_personnel AS medicalPersonnel,p.medins_id AS registeredInstitution,"
        + "c.company_name AS companyName,v.designated_number AS designatedNumber,COALESCE(i.institution_name,mi.dia_name) AS institutionName,COALESCE(v.hospital_grade,i.hospital_level,mi.dia_hos_level) AS hospitalGrade,v.medical_category AS medicalCategory,"
        + "v.admission_date AS admissionDate,v.discharge_date AS dischargeDate,v.disease_code AS diseaseCode,d.disease_name AS diseaseName,d.disease_reimbursement_standards AS diseaseReimbursementStandard,v.settlement_flag AS settlementFlag,"
        + "mi.dia_valid AS institutionValidFlag,mi.dia_starttime AS institutionValidFrom,mi.dia_endtime AS institutionValidTo "
        + "FROM t_personnel_visits_info v JOIN people p ON p.people_id=v.person_ID LEFT JOIN company c ON c.company_id=p.company_id LEFT JOIN t_medical_insititution mi ON mi.dia_id=v.designated_number LEFT JOIN ext_medical_institution_profile i ON i.institution_id=v.designated_number LEFT JOIN t_disease_ d ON d.disease_id=v.disease_code WHERE v.hospitalization_number=#{number}")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    Map<String,Object> calculationVisit(@Param("number") String number);

    @Select("SELECT settlement_flag FROM t_personnel_visits_info WHERE hospitalization_number=#{number} FOR UPDATE")
    String lockVisit(@Param("number") String number);

    @Select("SELECT p.chargeable_items_Category AS chargeableItemsCategory,p.project_coding AS projectCoding,p.project_name AS projectName,p.unit_price AS unitPrice,p.quantity,ROUND(p.unit_price*p.quantity,2) AS totalFee,"
        + "COALESCE(e.source_type,'MANUAL') AS sourceType,COALESCE(e.catalog_type,'MANUAL') AS catalogType,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_exp_level WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_exp_level ELSE NULL END AS expenseLevel,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_max_prize WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_max_prize ELSE NULL END AS maxPrice,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_approvalmark WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_approvalmark ELSE NULL END AS approvalMark,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_hos_level WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_hos_level ELSE NULL END AS itemHospitalLevel,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_specialmark ELSE NULL END AS specialMark,"
        + "CASE WHEN e.catalog_type='MEDICINE' AND m.med_id IS NOT NULL THEN 1 WHEN e.catalog_type='DIAGNOSIS' AND d.dia_id IS NOT NULL THEN 1 WHEN e.catalog_type='FACILITY' AND f.ser_id IS NOT NULL THEN 1 ELSE 0 END AS catalogExists,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_valid WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_valid WHEN e.catalog_type='FACILITY' THEN f.ser_valid ELSE NULL END AS validFlag,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_starttime WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_starttime WHEN e.catalog_type='FACILITY' THEN f.ser_starttime ELSE NULL END AS validFrom,"
        + "CASE WHEN e.catalog_type='MEDICINE' THEN m.med_endtime WHEN e.catalog_type='DIAGNOSIS' THEN d.dia_endtime WHEN e.catalog_type='FACILITY' THEN f.ser_endtime ELSE NULL END AS validTo "
        + "FROM t_prescription_details p LEFT JOIN ext_prescription_item e ON e.hospitalization_number=p.hospitalization_number AND e.chargeable_items_Category=p.chargeable_items_Category AND e.project_coding=p.project_coding "
        + "LEFT JOIN t_medicine m ON e.catalog_type='MEDICINE' AND m.med_id=p.project_coding LEFT JOIN t_diagnosis_project d ON e.catalog_type='DIAGNOSIS' AND d.dia_id=p.project_coding LEFT JOIN t_service_facilities f ON e.catalog_type='FACILITY' AND f.ser_id=p.project_coding "
        + "WHERE p.hospitalization_number=#{number} ORDER BY p.chargeable_items_Category,p.project_coding")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    List<Map<String,Object>> calculationItems(@Param("number") String number);

    @Select("SELECT minimum_payment_standard FROM t_minimum_payment_standard WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{personnel} AND hospital_level=#{hospitalLevel}")
    BigDecimal minimumStandard(@Param("medicalCategory") String medicalCategory,@Param("personnel") String personnel,@Param("hospitalLevel") String hospitalLevel);

    @Select("SELECT minimum_amount AS minimumAmount,maximum_amount AS maximumAmount,reimbursement_proportion AS reimbursementProportion FROM t_individual_segement_self_funded_ratio WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{personnel} AND hospital_level=#{hospitalLevel} ORDER BY minimum_amount")
    List<Map<String,Object>> segmentRatios(@Param("medicalCategory") String medicalCategory,@Param("personnel") String personnel,@Param("hospitalLevel") String hospitalLevel);

    @Select("SELECT capping_line_fee FROM t_capping_line WHERE medical_personnel_category=#{personnel}")
    BigDecimal cappingLine(@Param("personnel") String personnel);

    @Select("SELECT reimbursement_times AS reimbursementTimes,medical_expenses AS medicalExpenses,medicare_expenses AS medicareExpenses,personal_expenses AS personalExpenses FROM personal_annual_expenses WHERE people_id=#{personId} AND year=#{year}")
    Map<String,Object> annualExpense(@Param("personId") String personId,@Param("year") int year);

    @Insert("INSERT IGNORE INTO personal_annual_expenses (people_id,year,reimbursement_times,medical_expenses,medicare_expenses,personal_expenses) VALUES (#{personId},#{year},0,0,0,0)")
    int ensureAnnualExpense(@Param("personId") String personId,@Param("year") int year);

    @Select("SELECT reimbursement_times AS reimbursementTimes,medical_expenses AS medicalExpenses,medicare_expenses AS medicareExpenses,personal_expenses AS personalExpenses FROM personal_annual_expenses WHERE people_id=#{personId} AND year=#{year} FOR UPDATE")
    Map<String,Object> lockAnnualExpense(@Param("personId") String personId,@Param("year") int year);

    @Insert("INSERT INTO ext_reimbursement_settlement (settlement_no,hospitalization_number,transaction_type,original_settlement_id,total_fee,eligible_fee,over_limit_self_fee,deductible_self_fee,segment_self_fee,personal_fee,fund_fee,settlement_status,cancel_reason,operator_id,settled_at) VALUES (#{settlementNo},#{hospitalizationNumber},#{transactionType},#{originalSettlementId},#{totalFee},#{eligibleFee},#{overLimitSelfFee},#{deductibleSelfFee},#{segmentSelfFee},#{personalFee},#{fundFee},#{settlementStatus},#{cancelReason},#{operatorId},#{settledAt})")
    @Options(useGeneratedKeys=true,keyProperty="settlementId")
    int insertSettlement(SettlementRecord record);

    @Insert("INSERT INTO ext_settlement_fee_breakdown (settlement_id,fee_type,amount) VALUES (#{settlementId},#{feeType},#{amount})")
    int insertBreakdown(@Param("settlementId") Long settlementId,@Param("feeType") String feeType,@Param("amount") BigDecimal amount);

    @Insert("INSERT INTO ext_settlement_item_result (settlement_id,chargeable_items_category,project_coding,project_name,total_fee,eligible_fee,self_fee,calculation_note) VALUES (#{settlementId},#{item.chargeableItemsCategory},#{item.projectCoding},#{item.projectName},#{item.totalFee},#{item.eligibleFee},#{item.selfFee},#{item.calculationNote})")
    int insertItemResult(@Param("settlementId") Long settlementId,@Param("item") Map<String,Object> item);

    @Insert("INSERT INTO personal_annual_expenses (people_id,year,reimbursement_times,medical_expenses,medicare_expenses,personal_expenses) VALUES (#{personId},#{year},1,#{medical},#{fund},#{personal}) ON DUPLICATE KEY UPDATE reimbursement_times=reimbursement_times+1,medical_expenses=medical_expenses+VALUES(medical_expenses),medicare_expenses=medicare_expenses+VALUES(medicare_expenses),personal_expenses=personal_expenses+VALUES(personal_expenses)")
    int addAnnualExpense(@Param("personId") String personId,@Param("year") int year,@Param("medical") BigDecimal medical,@Param("fund") BigDecimal fund,@Param("personal") BigDecimal personal);

    @Update("UPDATE personal_annual_expenses SET reimbursement_times=GREATEST(reimbursement_times-1,0),medical_expenses=GREATEST(medical_expenses-#{medical},0),medicare_expenses=GREATEST(medicare_expenses-#{fund},0),personal_expenses=GREATEST(personal_expenses-#{personal},0) WHERE people_id=#{personId} AND year=#{year}")
    int subtractAnnualExpense(@Param("personId") String personId,@Param("year") int year,@Param("medical") BigDecimal medical,@Param("fund") BigDecimal fund,@Param("personal") BigDecimal personal);

    @Update("UPDATE t_personnel_visits_info SET settlement_flag=#{flag} WHERE hospitalization_number=#{number}")
    int updateVisitSettlementFlag(@Param("number") String number,@Param("flag") String flag);

    String SETTLEMENT_COLUMNS="s.settlement_id AS settlementId,s.settlement_no AS settlementNo,s.hospitalization_number AS hospitalizationNumber,s.transaction_type AS transactionType,s.original_settlement_id AS originalSettlementId,s.total_fee AS totalFee,s.eligible_fee AS eligibleFee,s.over_limit_self_fee AS overLimitSelfFee,s.deductible_self_fee AS deductibleSelfFee,s.segment_self_fee AS segmentSelfFee,s.personal_fee AS personalFee,s.fund_fee AS fundFee,s.settlement_status AS settlementStatus,s.cancel_reason AS cancelReason,s.operator_id AS operatorId,s.settled_at AS settledAt,v.person_ID AS personId,p.name AS personName,c.company_name AS companyName,v.medical_category AS medicalCategory,i.institution_name AS institutionName";
    String SETTLEMENT_FROM=" FROM ext_reimbursement_settlement s JOIN t_personnel_visits_info v ON v.hospitalization_number=s.hospitalization_number JOIN people p ON p.people_id=v.person_ID LEFT JOIN company c ON c.company_id=p.company_id LEFT JOIN ext_medical_institution_profile i ON i.institution_id=v.designated_number ";

    @Select("<script>SELECT "+SETTLEMENT_COLUMNS+SETTLEMENT_FROM+"<where><if test='keyword != null'>(s.settlement_no LIKE CONCAT('%',#{keyword},'%') OR s.hospitalization_number LIKE CONCAT('%',#{keyword},'%') OR v.person_ID LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%'))</if><if test='year != null'> AND YEAR(s.settled_at)=#{year}</if></where> ORDER BY s.settled_at DESC,s.settlement_id DESC</script>")
    List<Map<String,Object>> findSettlements(@Param("keyword") String keyword,@Param("year") Integer year);

    @Select("SELECT "+SETTLEMENT_COLUMNS+SETTLEMENT_FROM+"WHERE s.settlement_id=#{id}")
    Map<String,Object> findSettlement(@Param("id") Long id);

    @Select("SELECT settlement_id AS settlementId,settlement_no AS settlementNo,hospitalization_number AS hospitalizationNumber,transaction_type AS transactionType,original_settlement_id AS originalSettlementId,total_fee AS totalFee,eligible_fee AS eligibleFee,over_limit_self_fee AS overLimitSelfFee,deductible_self_fee AS deductibleSelfFee,segment_self_fee AS segmentSelfFee,personal_fee AS personalFee,fund_fee AS fundFee,settlement_status AS settlementStatus,cancel_reason AS cancelReason,operator_id AS operatorId,settled_at AS settledAt FROM ext_reimbursement_settlement WHERE settlement_id=#{id} FOR UPDATE")
    Map<String,Object> lockSettlement(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM ext_reimbursement_settlement WHERE original_settlement_id=#{id} AND transaction_type=0")
    int cancellationCount(@Param("id") Long id);

    @Update("UPDATE ext_reimbursement_settlement SET settlement_status='CANCELLED',cancel_reason=#{reason} WHERE settlement_id=#{id}")
    int cancelOriginal(@Param("id") Long id,@Param("reason") String reason);

    @Select("SELECT fee_type AS feeType,amount FROM ext_settlement_fee_breakdown WHERE settlement_id=#{id} ORDER BY fee_type")
    List<Map<String,Object>> breakdowns(@Param("id") Long id);

    @Select("SELECT chargeable_items_category AS chargeableItemsCategory,project_coding AS projectCoding,project_name AS projectName,total_fee AS totalFee,eligible_fee AS eligibleFee,self_fee AS selfFee,calculation_note AS calculationNote FROM ext_settlement_item_result WHERE settlement_id=#{id} ORDER BY result_id")
    List<Map<String,Object>> itemResults(@Param("id") Long id);
}
