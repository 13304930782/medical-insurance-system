package com.medical.insurance.dao;

import com.medical.insurance.model.PrescriptionForm;
import com.medical.insurance.model.VisitForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReimbursementMapper {
    String VISIT_COLUMNS="v.person_ID AS personId,p.name AS personName,p.medical_personnel AS medicalPersonnel,c.company_name AS companyName,v.hospitalization_number AS hospitalizationNumber,"
        +"v.designated_number AS designatedNumber,i.institution_name AS institutionName,v.medical_category AS medicalCategory,v.admission_date AS admissionDate,v.discharge_date AS dischargeDate,"
        +"v.disease_code AS diseaseCode,d.disease_name AS diseaseName,v.hospital_grade AS hospitalGrade,v.admission_code AS admissionCode,v.diagnosed_name AS diagnosedName,v.discharge_reason AS dischargeReason,v.settlement_flag AS settlementFlag";
    String VISIT_FROM=" FROM t_personnel_visits_info v LEFT JOIN people p ON p.people_id=v.person_ID LEFT JOIN company c ON c.company_id=p.company_id LEFT JOIN ext_medical_institution_profile i ON i.institution_id=v.designated_number LEFT JOIN t_disease_ d ON d.disease_id=v.disease_code ";

    @Select("<script>SELECT "+VISIT_COLUMNS+VISIT_FROM+"<if test='keyword != null'>WHERE v.hospitalization_number LIKE CONCAT('%',#{keyword},'%') OR v.person_ID LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%') OR i.institution_name LIKE CONCAT('%',#{keyword},'%')</if> ORDER BY v.admission_date DESC,v.hospitalization_number</script>")
    List<Map<String,Object>> findVisits(@Param("keyword")String keyword);
    @Select("SELECT "+VISIT_COLUMNS+VISIT_FROM+"WHERE v.hospitalization_number=#{number}") Map<String,Object> findVisit(@Param("number")String number);
    @Select("SELECT "+VISIT_COLUMNS+VISIT_FROM+"WHERE v.person_ID=#{personId} AND v.medical_category LIKE '%门诊%' AND COALESCE(v.settlement_flag,'未结算') NOT IN ('已结算','SETTLED') ORDER BY v.admission_date DESC,v.hospitalization_number DESC LIMIT 1")
    Map<String,Object> findReusableOutpatientVisit(@Param("personId")String personId);
    @Select("SELECT COUNT(*) FROM t_personnel_visits_info WHERE hospitalization_number=#{number}") int visitExists(@Param("number")String number);
    @Update("UPDATE ext_business_sequence SET current_value=LAST_INSERT_ID(current_value+1) WHERE sequence_key='OUTPATIENT'") int advanceOutpatientSequence();
    @Select("SELECT LAST_INSERT_ID()") long lastInsertId();
    @Insert("INSERT INTO t_personnel_visits_info (person_ID,hospitalization_number,designated_number,medical_category,admission_date,discharge_date,disease_code,hospital_grade,admission_code,diagnosed_name,discharge_reason,settlement_flag) VALUES (#{personId},#{hospitalizationNumber},#{designatedNumber},#{medicalCategory},#{admissionDate},#{dischargeDate},#{diseaseCode},#{hospitalGrade},#{admissionCode},#{diagnosedName},#{dischargeReason},#{settlementFlag})") int insertVisit(VisitForm form);
    @Update("UPDATE t_personnel_visits_info SET person_ID=#{personId},designated_number=#{designatedNumber},medical_category=#{medicalCategory},admission_date=#{admissionDate},discharge_date=#{dischargeDate},disease_code=#{diseaseCode},hospital_grade=#{hospitalGrade},admission_code=#{admissionCode},diagnosed_name=#{diagnosedName},discharge_reason=#{dischargeReason},settlement_flag=#{settlementFlag} WHERE hospitalization_number=#{hospitalizationNumber}") int updateVisit(VisitForm form);
    @Delete("DELETE FROM t_personnel_visits_info WHERE hospitalization_number=#{number}") int deleteVisit(@Param("number")String number);
    @Select("SELECT COUNT(*) FROM ext_reimbursement_settlement WHERE hospitalization_number=#{number}") int settlementHistoryCount(@Param("number")String number);

    @Select("SELECT p.hospitalization_number AS hospitalizationNumber,p.chargeable_items_Category AS chargeableItemsCategory,p.project_coding AS projectCoding,p.project_name AS projectName,p.unit_price AS unitPrice,p.quantity,p.amount,COALESCE(e.source_type,'CATALOG') AS sourceType,COALESCE(e.catalog_type,'MANUAL') AS catalogType FROM t_prescription_details p LEFT JOIN ext_prescription_item e ON e.hospitalization_number=p.hospitalization_number AND e.chargeable_items_Category=p.chargeable_items_Category AND e.project_coding=p.project_coding WHERE p.hospitalization_number=#{number} ORDER BY p.chargeable_items_Category,p.project_coding")
    List<Map<String,Object>> findPrescriptions(@Param("number")String number);
    @Select("SELECT COUNT(*) FROM t_prescription_details WHERE hospitalization_number=#{hospitalizationNumber} AND chargeable_items_Category=#{chargeableItemsCategory} AND project_coding=#{projectCoding}") int prescriptionExists(PrescriptionForm form);
    @Insert("INSERT INTO t_prescription_details (hospitalization_number,chargeable_items_Category,project_coding,project_name,unit_price,quantity,amount) VALUES (#{hospitalizationNumber},#{chargeableItemsCategory},#{projectCoding},#{projectName},#{unitPrice},#{quantity},#{amount})") int insertPrescription(PrescriptionForm form);
    @Update("UPDATE t_prescription_details SET project_name=#{projectName},unit_price=#{unitPrice},quantity=#{quantity},amount=#{amount} WHERE hospitalization_number=#{hospitalizationNumber} AND chargeable_items_Category=#{chargeableItemsCategory} AND project_coding=#{projectCoding}") int updatePrescription(PrescriptionForm form);
    @Insert("INSERT INTO ext_prescription_item (hospitalization_number,chargeable_items_Category,project_coding,source_type,catalog_type) VALUES (#{hospitalizationNumber},#{chargeableItemsCategory},#{projectCoding},#{sourceType},#{catalogType}) ON DUPLICATE KEY UPDATE source_type=VALUES(source_type),catalog_type=VALUES(catalog_type)") int upsertPrescriptionExtension(PrescriptionForm form);
    @Delete("DELETE FROM ext_prescription_item WHERE hospitalization_number=#{hospitalizationNumber} AND chargeable_items_Category=#{chargeableItemsCategory} AND project_coding=#{projectCoding}") int deletePrescriptionExtension(PrescriptionForm form);
    @Delete("DELETE FROM ext_prescription_item WHERE hospitalization_number=#{number}") int deleteVisitExtensions(@Param("number")String number);
    @Delete("DELETE FROM t_prescription_details WHERE hospitalization_number=#{number}") int deleteVisitPrescriptions(@Param("number")String number);
    @Delete("DELETE FROM t_prescription_details WHERE hospitalization_number=#{hospitalizationNumber} AND chargeable_items_Category=#{chargeableItemsCategory} AND project_coding=#{projectCoding}") int deletePrescription(PrescriptionForm form);

    @Select("SELECT COUNT(*) FROM people WHERE people_id=#{id}") int personExists(@Param("id")String id);
    @Select("SELECT COUNT(*) FROM ext_medical_institution_profile WHERE institution_id=#{id}") int institutionExists(@Param("id")String id);
    @Select("SELECT hospital_level FROM ext_medical_institution_profile WHERE institution_id=#{id}") String institutionHospitalLevel(@Param("id")String id);
    @Select("SELECT e.hospital_level AS hospitalLevel,o.dia_valid AS validFlag,o.dia_starttime AS validFrom,o.dia_endtime AS validTo FROM ext_medical_institution_profile e JOIN t_medical_insititution o ON o.dia_id=e.institution_id WHERE e.institution_id=#{id}") Map<String,Object> institution(@Param("id")String id);
    @Select("SELECT COUNT(*) FROM t_disease_ WHERE disease_id=#{id}") int diseaseExists(@Param("id")String id);
    @Select("SELECT med_name AS projectName,med_exp_type AS chargeableItemsCategory FROM t_medicine WHERE med_id=#{code}") Map<String,Object> medicine(@Param("code")String code);
    @Select("SELECT dia_name AS projectName,dia_exp_type AS chargeableItemsCategory FROM t_diagnosis_project WHERE dia_id=#{code}") Map<String,Object> diagnosis(@Param("code")String code);
    @Select("SELECT ser_name AS projectName,ser_exp_type AS chargeableItemsCategory FROM t_service_facilities WHERE ser_id=#{code}") Map<String,Object> facility(@Param("code")String code);
}
