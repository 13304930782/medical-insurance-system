package com.medical.insurance.dao;

import com.medical.insurance.model.DiseaseForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DiseaseMapper {

    String COLUMNS = "disease_id AS diseaseId, disease_name AS diseaseName, disease_type AS diseaseType, "
        + "disease_reimbursement_standards AS diseaseReimbursementStandards, notes";

    @Select("<script>SELECT COUNT(*) FROM t_disease_"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE disease_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR disease_name LIKE CONCAT('%',#{keyword},'%') OR disease_type LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT " + COLUMNS + " FROM t_disease_"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE disease_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR disease_name LIKE CONCAT('%',#{keyword},'%') OR disease_type LIKE CONCAT('%',#{keyword},'%')</if>"
        + " ORDER BY disease_id LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT " + COLUMNS + " FROM t_disease_ WHERE disease_id=#{diseaseId}")
    Map<String, Object> findById(@Param("diseaseId") String diseaseId);

    @Select("SELECT COUNT(*) FROM t_disease_ WHERE disease_id=#{diseaseId}")
    int exists(@Param("diseaseId") String diseaseId);

    @Insert("INSERT INTO t_disease_ (disease_id, disease_name, disease_type, disease_reimbursement_standards, notes) "
        + "VALUES (#{diseaseId}, #{diseaseName}, #{diseaseType}, #{diseaseReimbursementStandards}, #{notes})")
    int insert(DiseaseForm form);

    @Update("UPDATE t_disease_ SET disease_name=#{form.diseaseName}, disease_type=#{form.diseaseType}, "
        + "disease_reimbursement_standards=#{form.diseaseReimbursementStandards}, notes=#{form.notes} "
        + "WHERE disease_id=#{diseaseId}")
    int update(@Param("diseaseId") String diseaseId, @Param("form") DiseaseForm form);

    @Delete("DELETE FROM t_disease_ WHERE disease_id=#{diseaseId}")
    int delete(@Param("diseaseId") String diseaseId);
}
