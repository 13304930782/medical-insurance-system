package com.medical.insurance.dao;

import com.medical.insurance.model.DiagnosisForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DiagnosisMapper {

    String COLUMNS = "dia_id AS diaId, dia_name AS diaName, dia_exp_type AS diaExpType, "
        + "dia_exp_level AS diaExpLevel, dia_max_prize AS diaMaxPrize, dia_starttime AS diaStarttime, "
        + "dia_endtime AS diaEndtime, dia_valid AS diaValid, dia_hos_level AS diaHosLevel, "
        + "dia_approvalmark AS diaApprovalmark";

    @Select("<script>SELECT COUNT(*) FROM t_diagnosis_project"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE dia_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR dia_name LIKE CONCAT('%',#{keyword},'%') OR dia_exp_type LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT " + COLUMNS + " FROM t_diagnosis_project"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE dia_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR dia_name LIKE CONCAT('%',#{keyword},'%') OR dia_exp_type LIKE CONCAT('%',#{keyword},'%')</if>"
        + " ORDER BY dia_id LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT " + COLUMNS + " FROM t_diagnosis_project WHERE dia_id = #{diaId}")
    Map<String, Object> findById(@Param("diaId") String diaId);

    @Select("SELECT COUNT(*) FROM t_diagnosis_project WHERE dia_id = #{diaId}")
    int exists(@Param("diaId") String diaId);

    @Insert("INSERT INTO t_diagnosis_project (dia_id, dia_name, dia_exp_type, dia_exp_level, dia_max_prize, "
        + "dia_starttime, dia_endtime, dia_valid, dia_hos_level, dia_approvalmark) VALUES (#{diaId}, #{diaName}, "
        + "#{diaExpType}, #{diaExpLevel}, #{diaMaxPrize}, #{diaStarttime}, #{diaEndtime}, #{diaValid}, "
        + "#{diaHosLevel}, #{diaApprovalmark})")
    int insert(DiagnosisForm form);

    @Update("UPDATE t_diagnosis_project SET dia_name=#{form.diaName}, dia_exp_type=#{form.diaExpType}, "
        + "dia_exp_level=#{form.diaExpLevel}, dia_max_prize=#{form.diaMaxPrize}, "
        + "dia_starttime=#{form.diaStarttime}, dia_endtime=#{form.diaEndtime}, dia_valid=#{form.diaValid}, "
        + "dia_hos_level=#{form.diaHosLevel}, dia_approvalmark=#{form.diaApprovalmark} WHERE dia_id=#{diaId}")
    int update(@Param("diaId") String diaId, @Param("form") DiagnosisForm form);

    @Delete("DELETE FROM t_diagnosis_project WHERE dia_id = #{diaId}")
    int delete(@Param("diaId") String diaId);
}
