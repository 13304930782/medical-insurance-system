package com.medical.insurance.dao;

import com.medical.insurance.model.FacilityForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FacilityMapper {

    String COLUMNS = "ser_id AS serId, ser_name AS serName, ser_exp_type AS serExpType, "
        + "ser_starttime AS serStarttime, ser_endtime AS serEndtime, ser_valid AS serValid";

    @Select("<script>SELECT COUNT(*) FROM t_service_facilities"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE ser_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR ser_name LIKE CONCAT('%',#{keyword},'%') OR ser_exp_type LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT " + COLUMNS + " FROM t_service_facilities"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE ser_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR ser_name LIKE CONCAT('%',#{keyword},'%') OR ser_exp_type LIKE CONCAT('%',#{keyword},'%')</if>"
        + " ORDER BY ser_id LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT " + COLUMNS + " FROM t_service_facilities WHERE ser_id = #{serId}")
    Map<String, Object> findById(@Param("serId") String serId);

    @Select("SELECT COUNT(*) FROM t_service_facilities WHERE ser_id = #{serId}")
    int exists(@Param("serId") String serId);

    @Insert("INSERT INTO t_service_facilities (ser_id, ser_name, ser_exp_type, ser_starttime, ser_endtime, ser_valid) "
        + "VALUES (#{serId}, #{serName}, #{serExpType}, #{serStarttime}, #{serEndtime}, #{serValid})")
    int insert(FacilityForm form);

    @Update("UPDATE t_service_facilities SET ser_name=#{form.serName}, ser_exp_type=#{form.serExpType}, "
        + "ser_starttime=#{form.serStarttime}, ser_endtime=#{form.serEndtime}, ser_valid=#{form.serValid} "
        + "WHERE ser_id=#{serId}")
    int update(@Param("serId") String serId, @Param("form") FacilityForm form);

    @Delete("DELETE FROM t_service_facilities WHERE ser_id=#{serId}")
    int delete(@Param("serId") String serId);
}
