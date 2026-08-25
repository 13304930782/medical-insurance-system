package com.medical.insurance.dao;

import com.medical.insurance.model.PersonForm;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PersonMapper {

    String COLUMNS = "p.people_id AS peopleId, p.`ID_type` AS idType, p.`ID` AS id, p.name, p.sex, p.nationality, "
        + "DATE_FORMAT(p.brithday,'%Y-%m-%d') AS brithday, DATE_FORMAT(p.work_date,'%Y-%m-%d') AS workDate, "
        + "DATE_FORMAT(p.retirement_date,'%Y-%m-%d') AS retirementDate, p.retirement, "
        + "p.residence_type AS residenceType, p.residence_adress AS residenceAdress, p.education, "
        + "p.political_status AS politicalStatus, p.identity, p.employment, p.technical_position AS technicalPosition, "
        + "p.worker_level AS workerLevel, p.marriage, p.administrative_position AS administrativePosition, p.note, "
        + "p.company_id AS companyId, c.company_name AS companyName, p.medical_personnel AS medicalPersonnel, p.health, "
        + "p.model_worker AS modelWorker, p.cadre, p.civil_servant AS civilServant, p.authorized_strength AS authorizedStrength, "
        + "p.resident_type AS residentType, p.flexible_employment AS flexibleEmployment, p.migrant_worker AS migrantWorker, "
        + "p.employer, p.military_personnel AS militaryPersonnel, p.social_security_id AS socialSecurityId, "
        + "p.medins_id AS medinsId, i.institution_name AS medinsName";

    @Select("<script>SELECT COUNT(*) FROM people p"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE p.people_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR p.`ID` LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%')"
        + " OR p.company_id LIKE CONCAT('%',#{keyword},'%') OR p.social_security_id LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("keyword") String keyword);

    @Select("<script>SELECT " + COLUMNS + " FROM people p LEFT JOIN company c ON c.company_id=p.company_id "
        + "LEFT JOIN ext_medical_institution_profile i ON i.institution_id=p.medins_id"
        + "<if test='keyword != null and keyword != &quot;&quot;'> WHERE p.people_id LIKE CONCAT('%',#{keyword},'%')"
        + " OR p.`ID` LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%')"
        + " OR p.company_id LIKE CONCAT('%',#{keyword},'%') OR p.social_security_id LIKE CONCAT('%',#{keyword},'%')</if>"
        + " ORDER BY p.people_id LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String, Object>> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT " + COLUMNS + " FROM people p LEFT JOIN company c ON c.company_id=p.company_id "
        + "LEFT JOIN ext_medical_institution_profile i ON i.institution_id=p.medins_id WHERE p.people_id=#{peopleId}")
    Map<String, Object> findById(@Param("peopleId") String peopleId);

    @Select("SELECT COUNT(*) FROM people WHERE people_id=#{peopleId}")
    int exists(@Param("peopleId") String peopleId);

    @Select("SELECT COUNT(*) FROM people WHERE `ID`=#{id} AND people_id!=#{peopleId}")
    int documentExists(@Param("id") String id, @Param("peopleId") String peopleId);

    @Select("SELECT COUNT(*) FROM people WHERE social_security_id=#{socialSecurityId} AND people_id!=#{peopleId}")
    int socialSecurityExists(@Param("socialSecurityId") String socialSecurityId, @Param("peopleId") String peopleId);

    @Select("SELECT COUNT(*) FROM company WHERE company_id=#{companyId}")
    int companyExists(@Param("companyId") String companyId);

    @Select("SELECT COUNT(*) FROM ext_medical_institution_profile WHERE institution_id=#{medinsId}")
    int institutionExists(@Param("medinsId") String medinsId);

    @Insert("INSERT INTO people (people_id, `ID_type`, `ID`, name, sex, nationality, brithday, work_date, retirement_date, retirement, "
        + "residence_type, residence_adress, education, political_status, identity, employment, technical_position, worker_level, "
        + "marriage, administrative_position, note, company_id, medical_personnel, health, model_worker, cadre, civil_servant, "
        + "authorized_strength, resident_type, flexible_employment, migrant_worker, employer, military_personnel, social_security_id, medins_id) "
        + "VALUES (#{peopleId}, #{idType}, #{id}, #{name}, #{sex}, #{nationality}, #{brithday}, #{workDate}, #{retirementDate}, #{retirement}, "
        + "#{residenceType}, #{residenceAdress}, #{education}, #{politicalStatus}, #{identity}, #{employment}, #{technicalPosition}, #{workerLevel}, "
        + "#{marriage}, #{administrativePosition}, #{note}, #{companyId}, #{medicalPersonnel}, #{health}, #{modelWorker}, #{cadre}, #{civilServant}, "
        + "#{authorizedStrength}, #{residentType}, #{flexibleEmployment}, #{migrantWorker}, #{employer}, #{militaryPersonnel}, #{socialSecurityId}, #{medinsId})")
    int insert(PersonForm form);

    @Update("UPDATE people SET `ID_type`=#{form.idType}, `ID`=#{form.id}, name=#{form.name}, sex=#{form.sex}, nationality=#{form.nationality}, "
        + "brithday=#{form.brithday}, work_date=#{form.workDate}, retirement_date=#{form.retirementDate}, retirement=#{form.retirement}, "
        + "residence_type=#{form.residenceType}, residence_adress=#{form.residenceAdress}, education=#{form.education}, "
        + "political_status=#{form.politicalStatus}, identity=#{form.identity}, employment=#{form.employment}, "
        + "technical_position=#{form.technicalPosition}, worker_level=#{form.workerLevel}, marriage=#{form.marriage}, "
        + "administrative_position=#{form.administrativePosition}, note=#{form.note}, company_id=#{form.companyId}, "
        + "medical_personnel=#{form.medicalPersonnel}, health=#{form.health}, model_worker=#{form.modelWorker}, cadre=#{form.cadre}, "
        + "civil_servant=#{form.civilServant}, authorized_strength=#{form.authorizedStrength}, resident_type=#{form.residentType}, "
        + "flexible_employment=#{form.flexibleEmployment}, migrant_worker=#{form.migrantWorker}, employer=#{form.employer}, "
        + "military_personnel=#{form.militaryPersonnel}, social_security_id=#{form.socialSecurityId}, medins_id=#{form.medinsId} "
        + "WHERE people_id=#{peopleId}")
    int update(@Param("peopleId") String peopleId, @Param("form") PersonForm form);

    @Delete("DELETE FROM people WHERE people_id=#{peopleId}")
    int delete(@Param("peopleId") String peopleId);
}
