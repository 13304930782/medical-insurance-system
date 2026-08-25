package com.medical.insurance.dao;

import com.medical.insurance.model.InstitutionForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstitutionMapper {

    String COLUMNS = "institution_id AS institutionId, institution_name AS institutionName, "
        + "hospital_level AS hospitalLevel, institution_type AS institutionType, postcode, "
        + "legal_representative_name AS legalRepresentativeName, "
        + "legal_representative_mobile AS legalRepresentativeMobile, contact_name AS contactName, "
        + "contact_phone AS contactPhone, contact_mobile AS contactMobile, address, notes";

    @Select("<script>SELECT COUNT(*) FROM ext_medical_institution_profile"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE institution_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR institution_name LIKE CONCAT('%',#{keyword},'%') OR institution_type LIKE CONCAT('%',#{keyword},'%')"
        + " OR address LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT " + COLUMNS + " FROM ext_medical_institution_profile"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE institution_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR institution_name LIKE CONCAT('%',#{keyword},'%') OR institution_type LIKE CONCAT('%',#{keyword},'%')"
        + " OR address LIKE CONCAT('%',#{keyword},'%')</if>"
        + " ORDER BY institution_id LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT " + COLUMNS + " FROM ext_medical_institution_profile WHERE institution_id=#{institutionId}")
    Map<String, Object> findById(@Param("institutionId") String institutionId);

    @Select("SELECT COUNT(*) FROM ext_medical_institution_profile WHERE institution_id=#{institutionId}")
    int existsProfile(@Param("institutionId") String institutionId);

    @Select("SELECT COUNT(*) FROM t_medical_insititution WHERE dia_id=#{institutionId}")
    int existsOriginal(@Param("institutionId") String institutionId);

    @Insert("INSERT INTO t_medical_insititution "
        + "(dia_id, dia_name, dia_exp_type, dia_max_prize, dia_valid, dia_hos_level) "
        + "VALUES (#{institutionId}, #{institutionName}, #{institutionType}, 0, '有效', #{hospitalLevel})")
    int insertOriginal(InstitutionForm form);

    @Insert("INSERT INTO ext_medical_institution_profile "
        + "(institution_id, institution_name, hospital_level, institution_type, postcode, "
        + "legal_representative_name, legal_representative_mobile, contact_name, contact_phone, "
        + "contact_mobile, address, notes) VALUES (#{institutionId}, #{institutionName}, #{hospitalLevel}, "
        + "#{institutionType}, #{postcode}, #{legalRepresentativeName}, #{legalRepresentativeMobile}, "
        + "#{contactName}, #{contactPhone}, #{contactMobile}, #{address}, #{notes})")
    int insertProfile(InstitutionForm form);

    @Update("UPDATE t_medical_insititution SET dia_name=#{form.institutionName}, "
        + "dia_exp_type=#{form.institutionType}, dia_hos_level=#{form.hospitalLevel} WHERE dia_id=#{institutionId}")
    int updateOriginal(@Param("institutionId") String institutionId, @Param("form") InstitutionForm form);

    @Update("UPDATE ext_medical_institution_profile SET institution_name=#{form.institutionName}, "
        + "hospital_level=#{form.hospitalLevel}, institution_type=#{form.institutionType}, postcode=#{form.postcode}, "
        + "legal_representative_name=#{form.legalRepresentativeName}, "
        + "legal_representative_mobile=#{form.legalRepresentativeMobile}, contact_name=#{form.contactName}, "
        + "contact_phone=#{form.contactPhone}, contact_mobile=#{form.contactMobile}, address=#{form.address}, "
        + "notes=#{form.notes} WHERE institution_id=#{institutionId}")
    int updateProfile(@Param("institutionId") String institutionId, @Param("form") InstitutionForm form);

    @Delete("DELETE FROM t_medical_insititution WHERE dia_id=#{institutionId}")
    int delete(@Param("institutionId") String institutionId);
}
