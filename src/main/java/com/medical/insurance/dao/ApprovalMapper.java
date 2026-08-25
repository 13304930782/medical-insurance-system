package com.medical.insurance.dao;

import com.medical.insurance.model.InstitutionApprovalForm;
import com.medical.insurance.model.SpecialApprovalForm;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApprovalMapper {
    String INSTITUTION_COLUMNS = "a.approval_number AS approvalNumber,a.person_ID AS personId,p.name AS personName,c.company_name AS companyName,"
        + "a.approval_category AS approvalCategory,a.start_date AS startDate,a.termination_date AS terminationDate,"
        + "a.medical_institution_code AS medicalInstitutionCode,i.institution_name AS medicalInstitutionName,"
        + "a.approval_opinions AS approvalOpinions,a.approver,a.approval_date AS approvalDate,a.approval_flag AS approvalFlag";

    @Select("<script>SELECT " + INSTITUTION_COLUMNS + " FROM t_application_info a LEFT JOIN people p ON p.people_id=a.person_ID LEFT JOIN company c ON c.company_id=p.company_id "
        + "LEFT JOIN ext_medical_institution_profile i ON i.institution_id=a.medical_institution_code "
        + "<if test='keyword != null'>WHERE a.approval_number LIKE CONCAT('%',#{keyword},'%') OR a.person_ID LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%') OR i.institution_name LIKE CONCAT('%',#{keyword},'%')</if> ORDER BY a.approval_date DESC,a.approval_number</script>")
    List<Map<String,Object>> findInstitutionApprovals(@Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_application_info WHERE approval_number=#{approvalNumber}") int institutionApprovalExists(@Param("approvalNumber") String approvalNumber);
    @Insert("INSERT INTO t_application_info (approval_number,person_ID,approval_category,start_date,termination_date,medical_institution_code,approval_opinions,approver,approval_date,approval_flag) VALUES (#{approvalNumber},#{personId},#{approvalCategory},#{startDate},#{terminationDate},#{medicalInstitutionCode},#{approvalOpinions},#{approver},#{approvalDate},#{approvalFlag})") int insertInstitutionApproval(InstitutionApprovalForm form);
    @Update("UPDATE t_application_info SET person_ID=#{personId},approval_category=#{approvalCategory},start_date=#{startDate},termination_date=#{terminationDate},medical_institution_code=#{medicalInstitutionCode},approval_opinions=#{approvalOpinions},approver=#{approver},approval_date=#{approvalDate},approval_flag=#{approvalFlag} WHERE approval_number=#{approvalNumber}") int updateInstitutionApproval(InstitutionApprovalForm form);
    @Delete("DELETE FROM t_application_info WHERE approval_number=#{approvalNumber}") int deleteInstitutionApproval(@Param("approvalNumber") String approvalNumber);

    @Select("<script>SELECT s.approval_number AS approvalNumber,s.person_ID AS personId,p.name AS personName,c.company_name AS companyName,s.approval_category AS approvalCategory,"
        + "s.start_date AS startDate,s.termination_date AS terminationDate,s.drug_Code AS drugCode,s.approval_opinions AS approvalOpinions,s.approver,s.approval_date AS approvalDate,s.approval_flag AS approvalFlag,"
        + "COALESCE(e.item_type,'MEDICINE') AS itemType,COALESCE(e.project_code,s.drug_Code) AS projectCode,COALESCE(m.med_name,d.dia_name) AS projectName "
        + "FROM t_special_approval s LEFT JOIN people p ON p.people_id=s.person_ID LEFT JOIN company c ON c.company_id=p.company_id LEFT JOIN ext_special_approval_item e ON e.approval_number=s.approval_number "
        + "LEFT JOIN t_medicine m ON m.med_id=COALESCE(e.project_code,s.drug_Code) LEFT JOIN t_diagnosis_project d ON d.dia_id=e.project_code AND e.item_type='DIAGNOSIS' "
        + "<if test='keyword != null'>WHERE s.approval_number LIKE CONCAT('%',#{keyword},'%') OR s.person_ID LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%') OR COALESCE(e.project_code,s.drug_Code) LIKE CONCAT('%',#{keyword},'%')</if> ORDER BY s.approval_date DESC,s.approval_number</script>")
    List<Map<String,Object>> findSpecialApprovals(@Param("keyword") String keyword);

    @Select("<script>SELECT med_id AS projectCode,med_name AS projectName,med_exp_type AS chargeableItemsCategory,med_exp_level AS expenseLevel,'MEDICINE' AS itemType "
        + "FROM t_medicine WHERE (COALESCE(med_approvalmark,'') IN ('需要审批','是','1') OR COALESCE(med_specialmark,'') IN ('需要审批','是','特检特治','1')) "
        + "<if test='keyword != null'>AND (med_id LIKE CONCAT('%',#{keyword},'%') OR med_name LIKE CONCAT('%',#{keyword},'%'))</if> ORDER BY med_id</script>")
    List<Map<String,Object>> findSpecialMedicineProjects(@Param("keyword") String keyword);

    @Select("<script>SELECT dia_id AS projectCode,dia_name AS projectName,dia_exp_type AS chargeableItemsCategory,dia_exp_level AS expenseLevel,'DIAGNOSIS' AS itemType "
        + "FROM t_diagnosis_project WHERE (COALESCE(dia_approvalmark,'') IN ('需要审批','是','1') OR dia_exp_type IN ('特殊检查费','特殊治疗费','特检费','特治费')) "
        + "<if test='keyword != null'>AND (dia_id LIKE CONCAT('%',#{keyword},'%') OR dia_name LIKE CONCAT('%',#{keyword},'%'))</if> ORDER BY dia_id</script>")
    List<Map<String,Object>> findSpecialDiagnosisProjects(@Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_special_approval WHERE approval_number=#{approvalNumber}") int specialApprovalExists(@Param("approvalNumber") String approvalNumber);
    @Insert("INSERT INTO t_special_approval (approval_number,person_ID,approval_category,start_date,termination_date,drug_Code,approval_opinions,approver,approval_date,approval_flag) VALUES (#{approvalNumber},#{personId},#{approvalCategory},#{startDate},#{terminationDate},#{drugCode},#{approvalOpinions},#{approver},#{approvalDate},#{approvalFlag})") int insertSpecialApproval(SpecialApprovalForm form);
    @Update("UPDATE t_special_approval SET person_ID=#{personId},approval_category=#{approvalCategory},start_date=#{startDate},termination_date=#{terminationDate},drug_Code=#{drugCode},approval_opinions=#{approvalOpinions},approver=#{approver},approval_date=#{approvalDate},approval_flag=#{approvalFlag} WHERE approval_number=#{approvalNumber}") int updateSpecialApproval(SpecialApprovalForm form);
    @Delete("DELETE FROM t_special_approval WHERE approval_number=#{approvalNumber}") int deleteSpecialApproval(@Param("approvalNumber") String approvalNumber);
    @Insert("INSERT INTO ext_special_approval_item (approval_number,item_type,project_code) VALUES (#{approvalNumber},#{itemType},#{projectCode}) ON DUPLICATE KEY UPDATE item_type=VALUES(item_type),project_code=VALUES(project_code)") int upsertSpecialItem(SpecialApprovalForm form);

    @Select("SELECT COUNT(*) FROM people WHERE people_id=#{id}") int personExists(@Param("id") String id);
    @Select("SELECT COUNT(*) FROM ext_medical_institution_profile WHERE institution_id=#{id}") int institutionExists(@Param("id") String id);
    @Select("SELECT COUNT(*) FROM t_medicine WHERE med_id=#{code}") int medicineExists(@Param("code") String code);
    @Select("SELECT COUNT(*) FROM t_diagnosis_project WHERE dia_id=#{code}") int diagnosisExists(@Param("code") String code);
    @Select("SELECT COUNT(*) FROM t_medicine WHERE med_id=#{code} AND (COALESCE(med_approvalmark,'') IN ('需要审批','是','1') OR COALESCE(med_specialmark,'') IN ('需要审批','是','特检特治','1'))") int medicineRequiresSpecialApproval(@Param("code") String code);
    @Select("SELECT COUNT(*) FROM t_diagnosis_project WHERE dia_id=#{code} AND (COALESCE(dia_approvalmark,'') IN ('需要审批','是','1') OR dia_exp_type IN ('特殊检查费','特殊治疗费','特检费','特治费'))") int diagnosisRequiresSpecialApproval(@Param("code") String code);

    @Select("SELECT COUNT(*) FROM t_application_info WHERE person_ID=#{personId} AND medical_institution_code=#{institutionCode} AND approval_flag IN ('审批通过','通过','有效','1') AND (start_date IS NULL OR start_date <= #{visitDate}) AND (termination_date IS NULL OR termination_date >= #{visitDate})")
    int hasActiveInstitutionApproval(@Param("personId") String personId,@Param("institutionCode") String institutionCode,@Param("visitDate") LocalDate visitDate);
    @Select("SELECT COUNT(*) FROM t_special_approval s LEFT JOIN ext_special_approval_item e ON e.approval_number=s.approval_number WHERE s.person_ID=#{personId} AND COALESCE(e.item_type,'MEDICINE')=#{itemType} AND COALESCE(e.project_code,s.drug_Code)=#{projectCode} AND s.approval_flag IN ('审批通过','通过','有效','1') AND (s.start_date IS NULL OR s.start_date <= #{visitDate}) AND (s.termination_date IS NULL OR s.termination_date >= #{visitDate})")
    int hasActiveSpecialApproval(@Param("personId") String personId,@Param("itemType") String itemType,@Param("projectCode") String projectCode,@Param("visitDate") LocalDate visitDate);
}
