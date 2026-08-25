package com.medical.insurance.dao;

import com.medical.insurance.model.CappingLineForm;
import com.medical.insurance.model.MinimumPaymentStandardForm;
import com.medical.insurance.model.SegmentRatioForm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TreatmentParameterMapper {

    @Select("<script>SELECT medical_personnel_category AS medicalPersonnelCategory, capping_line_fee AS cappingLineFee "
        + "FROM t_capping_line <if test='keyword != null'>WHERE medical_personnel_category LIKE CONCAT('%',#{keyword},'%')</if> "
        + "ORDER BY medical_personnel_category</script>")
    List<Map<String, Object>> findCappingLines(@Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_capping_line WHERE medical_personnel_category=#{category}")
    int cappingExists(@Param("category") String category);

    @Insert("INSERT INTO t_capping_line (medical_personnel_category,capping_line_fee) VALUES (#{medicalPersonnelCategory},#{cappingLineFee})")
    int insertCapping(CappingLineForm form);

    @Update("UPDATE t_capping_line SET capping_line_fee=#{fee} WHERE medical_personnel_category=#{category}")
    int updateCapping(@Param("category") String category, @Param("fee") BigDecimal fee);

    @Delete("DELETE FROM t_capping_line WHERE medical_personnel_category=#{category}")
    int deleteCapping(@Param("category") String category);

    @Select("<script>SELECT medical_category AS medicalCategory, medical_personnel_category AS medicalPersonnelCategory, "
        + "hospital_level AS hospitalLevel, minimum_payment_standard AS minimumPaymentStandard FROM t_minimum_payment_standard "
        + "<if test='keyword != null'>WHERE medical_category LIKE CONCAT('%',#{keyword},'%') OR medical_personnel_category LIKE CONCAT('%',#{keyword},'%') OR hospital_level LIKE CONCAT('%',#{keyword},'%')</if> "
        + "ORDER BY medical_category,medical_personnel_category,hospital_level</script>")
    List<Map<String, Object>> findMinimumStandards(@Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_minimum_payment_standard WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel}")
    int minimumExists(MinimumPaymentStandardForm form);

    @Insert("INSERT INTO t_minimum_payment_standard (medical_category,medical_personnel_category,hospital_level,minimum_payment_standard) "
        + "VALUES (#{medicalCategory},#{medicalPersonnelCategory},#{hospitalLevel},#{minimumPaymentStandard})")
    int insertMinimum(MinimumPaymentStandardForm form);

    @Update("UPDATE t_minimum_payment_standard SET minimum_payment_standard=#{minimumPaymentStandard} WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel}")
    int updateMinimum(MinimumPaymentStandardForm form);

    @Delete("DELETE FROM t_minimum_payment_standard WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel}")
    int deleteMinimum(@Param("medicalCategory") String medicalCategory, @Param("medicalPersonnelCategory") String medicalPersonnelCategory, @Param("hospitalLevel") String hospitalLevel);

    @Select("<script>SELECT medical_category AS medicalCategory, medical_personnel_category AS medicalPersonnelCategory, hospital_level AS hospitalLevel, "
        + "maximum_amount AS maximumAmount, minimum_amount AS minimumAmount, reimbursement_proportion AS reimbursementProportion "
        + "FROM t_individual_segement_self_funded_ratio <if test='keyword != null'>WHERE medical_category LIKE CONCAT('%',#{keyword},'%') OR medical_personnel_category LIKE CONCAT('%',#{keyword},'%') OR hospital_level LIKE CONCAT('%',#{keyword},'%')</if> "
        + "ORDER BY medical_category,medical_personnel_category,hospital_level,minimum_amount</script>")
    List<Map<String, Object>> findSegmentRatios(@Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_individual_segement_self_funded_ratio WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel} AND minimum_amount=#{minimumAmount} AND maximum_amount=#{maximumAmount}")
    int segmentExists(SegmentRatioForm form);

    @Select("SELECT COUNT(*) FROM t_individual_segement_self_funded_ratio WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel} "
        + "AND minimum_amount < #{maximumAmount} AND maximum_amount > #{minimumAmount} "
        + "AND NOT (minimum_amount=#{minimumAmount} AND maximum_amount=#{maximumAmount})")
    int overlappingSegments(SegmentRatioForm form);

    @Insert("INSERT INTO t_individual_segement_self_funded_ratio (medical_category,medical_personnel_category,hospital_level,maximum_amount,minimum_amount,reimbursement_proportion) "
        + "VALUES (#{medicalCategory},#{medicalPersonnelCategory},#{hospitalLevel},#{maximumAmount},#{minimumAmount},#{reimbursementProportion})")
    int insertSegment(SegmentRatioForm form);

    @Update("UPDATE t_individual_segement_self_funded_ratio SET reimbursement_proportion=#{reimbursementProportion} WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel} AND minimum_amount=#{minimumAmount} AND maximum_amount=#{maximumAmount}")
    int updateSegment(SegmentRatioForm form);

    @Delete("DELETE FROM t_individual_segement_self_funded_ratio WHERE medical_category=#{medicalCategory} AND medical_personnel_category=#{medicalPersonnelCategory} AND hospital_level=#{hospitalLevel} AND minimum_amount=#{minimumAmount} AND maximum_amount=#{maximumAmount}")
    int deleteSegment(@Param("medicalCategory") String medicalCategory, @Param("medicalPersonnelCategory") String medicalPersonnelCategory, @Param("hospitalLevel") String hospitalLevel, @Param("minimumAmount") BigDecimal minimumAmount, @Param("maximumAmount") BigDecimal maximumAmount);
}
