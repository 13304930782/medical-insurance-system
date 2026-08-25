package com.medical.insurance.dao;

import com.medical.insurance.model.MedicineForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MedicineMapper {

    String COLUMNS = "med_id AS medId, med_name AS medName, med_exp_type AS medExpType, "
        + "med_exp_level AS medExpLevel, med_measurement AS medMeasurement, med_max_prize AS medMaxPrize, "
        + "med_approvalmark AS medApprovalmark, med_hos_level AS medHosLevel, med_size AS medSize, "
        + "med_tradename AS medTradename, med_starttime AS medStarttime, med_endtime AS medEndtime, "
        + "med_valid AS medValid, med_specialmark AS medSpecialmark";

    @Select("<script>SELECT COUNT(*) FROM t_medicine"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE med_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR med_name LIKE CONCAT('%',#{keyword},'%') OR med_tradename LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT " + COLUMNS + " FROM t_medicine"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE med_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR med_name LIKE CONCAT('%',#{keyword},'%') OR med_tradename LIKE CONCAT('%',#{keyword},'%')</if>"
        + " ORDER BY med_id LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT " + COLUMNS + " FROM t_medicine WHERE med_id = #{medId}")
    Map<String, Object> findById(@Param("medId") String medId);

    @Select("SELECT COUNT(*) FROM t_medicine WHERE med_id = #{medId}")
    int exists(@Param("medId") String medId);

    @Insert("INSERT INTO t_medicine (med_id, med_name, med_exp_type, med_exp_level, med_measurement, "
        + "med_max_prize, med_approvalmark, med_hos_level, med_size, med_tradename, med_starttime, "
        + "med_endtime, med_valid, med_specialmark) VALUES (#{medId}, #{medName}, #{medExpType}, "
        + "#{medExpLevel}, #{medMeasurement}, #{medMaxPrize}, #{medApprovalmark}, #{medHosLevel}, "
        + "#{medSize}, #{medTradename}, #{medStarttime}, #{medEndtime}, #{medValid}, #{medSpecialmark})")
    int insert(MedicineForm form);

    @Update("UPDATE t_medicine SET med_name=#{form.medName}, med_exp_type=#{form.medExpType}, "
        + "med_exp_level=#{form.medExpLevel}, med_measurement=#{form.medMeasurement}, "
        + "med_max_prize=#{form.medMaxPrize}, med_approvalmark=#{form.medApprovalmark}, "
        + "med_hos_level=#{form.medHosLevel}, med_size=#{form.medSize}, med_tradename=#{form.medTradename}, "
        + "med_starttime=#{form.medStarttime}, med_endtime=#{form.medEndtime}, med_valid=#{form.medValid}, "
        + "med_specialmark=#{form.medSpecialmark} WHERE med_id=#{medId}")
    int update(@Param("medId") String medId, @Param("form") MedicineForm form);

    @Delete("DELETE FROM t_medicine WHERE med_id = #{medId}")
    int delete(@Param("medId") String medId);
}
