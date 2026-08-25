package com.medical.insurance.service.impl;

import com.medical.insurance.dao.PersonMapper;
import com.medical.insurance.exception.PersonBusinessException;
import com.medical.insurance.model.PersonForm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

    private final PersonMapper personMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public PersonService(PersonMapper personMapper, AuthService authService, SystemMapper systemMapper) {
        this.personMapper = personMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        String normalizedKeyword = normalizeKeyword(keyword);
        long total = personMapper.count(normalizedKeyword);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", personMapper.findPage(normalizedKeyword, (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String peopleId) {
        Map<String, Object> person = personMapper.findById(peopleId);
        if (person == null) throw new PersonBusinessException("未找到该人员");
        return person;
    }

    @Transactional
    public void create(PersonForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
        if (personMapper.exists(form.getPeopleId()) > 0) throw new PersonBusinessException("个人ID已存在");
        validateRelationsAndUniqueFields(form);
        personMapper.insert(form);
        record(authService.currentUserId(request), "CREATE", form.getPeopleId(), "新增个人：" + form.getName(), request);
    }

    @Transactional
    public void update(String peopleId, PersonForm form, HttpServletRequest request) {
        Map<String, Object> existing = detail(peopleId);
        form.setPeopleId(peopleId);
        if (isBlank(form.getSocialSecurityId())) form.setSocialSecurityId(stringValue(existing.get("socialSecurityId")));
        normalizeAndValidate(form, false);
        validateRelationsAndUniqueFields(form);
        if (personMapper.update(peopleId, form) == 0) throw new PersonBusinessException("未找到该人员");
        record(authService.currentUserId(request), "UPDATE", peopleId, "修改个人：" + form.getName(), request);
    }

    @Transactional
    public void delete(String peopleId, HttpServletRequest request) {
        Map<String, Object> person = detail(peopleId);
        if (personMapper.delete(peopleId) == 0) throw new PersonBusinessException("未找到该人员");
        record(authService.currentUserId(request), "DELETE", peopleId, "删除个人：" + person.get("name"), request);
    }

    private void normalizeAndValidate(PersonForm form, boolean creating) {
        form.setPeopleId(trim(form.getPeopleId()));
        form.setIdType(trim(form.getIdType()));
        form.setId(trim(form.getId()));
        form.setName(trim(form.getName()));
        form.setSex(trim(form.getSex()));
        form.setNationality(trim(form.getNationality()));
        form.setRetirement(trim(form.getRetirement()));
        form.setResidenceType(trim(form.getResidenceType()));
        form.setResidenceAdress(trim(form.getResidenceAdress()));
        form.setEducation(trim(form.getEducation()));
        form.setPoliticalStatus(trim(form.getPoliticalStatus()));
        form.setIdentity(trim(form.getIdentity()));
        form.setEmployment(trim(form.getEmployment()));
        form.setTechnicalPosition(trim(form.getTechnicalPosition()));
        form.setWorkerLevel(trim(form.getWorkerLevel()));
        form.setMarriage(trim(form.getMarriage()));
        form.setAdministrativePosition(trim(form.getAdministrativePosition()));
        form.setNote(trim(form.getNote()));
        form.setCompanyId(trim(form.getCompanyId()));
        form.setMedicalPersonnel(trim(form.getMedicalPersonnel()));
        form.setHealth(trim(form.getHealth()));
        form.setModelWorker(trim(form.getModelWorker()));
        form.setCadre(trim(form.getCadre()));
        form.setCivilServant(trim(form.getCivilServant()));
        form.setAuthorizedStrength(trim(form.getAuthorizedStrength()));
        form.setResidentType(trim(form.getResidentType()));
        form.setFlexibleEmployment(trim(form.getFlexibleEmployment()));
        form.setMigrantWorker(trim(form.getMigrantWorker()));
        form.setEmployer(trim(form.getEmployer()));
        form.setMilitaryPersonnel(trim(form.getMilitaryPersonnel()));
        form.setSocialSecurityId(trim(form.getSocialSecurityId()));
        form.setMedinsId(trim(form.getMedinsId()));
        if (creating && isBlank(form.getPeopleId())) throw new PersonBusinessException("个人ID不能为空");
        if (isBlank(form.getName())) throw new PersonBusinessException("姓名不能为空");
        if (isBlank(form.getSocialSecurityId())) form.setSocialSecurityId(generateSocialSecurityId());
    }

    private void validateRelationsAndUniqueFields(PersonForm form) {
         if (!isBlank(form.getId()) && personMapper.documentExists(form.getId(), form.getPeopleId()) > 0) {
            throw new PersonBusinessException("证件编号已存在");
        }
         if (personMapper.socialSecurityExists(form.getSocialSecurityId(), form.getPeopleId()) > 0) {
            throw new PersonBusinessException("社保卡号已存在");
        }
         if (!isBlank(form.getCompanyId()) && personMapper.companyExists(form.getCompanyId()) == 0) {
            throw new PersonBusinessException("单位编码不存在，请先维护单位信息");
        }
         if (!isBlank(form.getMedinsId()) && personMapper.institutionExists(form.getMedinsId()) == 0) {
            throw new PersonBusinessException("定点医疗机构编码不存在");
        }
    }

    private String generateSocialSecurityId() {
        String value;
        do {
            value = UUID.randomUUID().toString().replace("-", "");
        } while (personMapper.socialSecurityExists(value, "") > 0);
        return value;
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "个人基本信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private String trim(String value) { return isBlank(value) ? null : value.trim(); }
    private String stringValue(Object value) { return value == null ? null : String.valueOf(value); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
