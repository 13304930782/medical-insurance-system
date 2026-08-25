package com.medical.insurance.service.impl;

import com.medical.insurance.dao.InstitutionMapper;
import com.medical.insurance.exception.InstitutionBusinessException;
import com.medical.insurance.model.InstitutionForm;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {

    private final InstitutionMapper institutionMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public InstitutionService(InstitutionMapper institutionMapper, AuthService authService, SystemMapper systemMapper) {
        this.institutionMapper = institutionMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        String normalizedKeyword = normalizeKeyword(keyword);
        long total = institutionMapper.count(normalizedKeyword);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", institutionMapper.findPage(normalizedKeyword, (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String institutionId) {
        Map<String, Object> institution = institutionMapper.findById(institutionId);
        if (institution == null) throw new InstitutionBusinessException("未找到该定点医疗机构");
        return institution;
    }

    @Transactional
    public void create(InstitutionForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
        if (institutionMapper.existsOriginal(form.getInstitutionId()) > 0
             || institutionMapper.existsProfile(form.getInstitutionId()) > 0) {
            throw new InstitutionBusinessException("定点医疗机构编号已存在");
        }
        institutionMapper.insertOriginal(form);
        institutionMapper.insertProfile(form);
        record(authService.currentUserId(request), "CREATE", form.getInstitutionId(),
            "新增定点医疗机构：" + form.getInstitutionName(), request);
    }

    @Transactional
    public void update(String institutionId, InstitutionForm form, HttpServletRequest request) {
        form.setInstitutionId(institutionId);
        normalizeAndValidate(form, false);
         if (institutionMapper.updateProfile(institutionId, form) == 0) {
            throw new InstitutionBusinessException("未找到该定点医疗机构");
        }
        institutionMapper.updateOriginal(institutionId, form);
        record(authService.currentUserId(request), "UPDATE", institutionId,
            "修改定点医疗机构：" + form.getInstitutionName(), request);
    }

    @Transactional
    public void delete(String institutionId, HttpServletRequest request) {
        Map<String, Object> institution = detail(institutionId);
         if (institutionMapper.delete(institutionId) == 0) {
            throw new InstitutionBusinessException("未找到该定点医疗机构");
        }
        record(authService.currentUserId(request), "DELETE", institutionId,
            "删除定点医疗机构：" + institution.get("institutionName"), request);
    }

    private void normalizeAndValidate(InstitutionForm form, boolean creating) {
        form.setInstitutionId(trim(form.getInstitutionId()));
        form.setInstitutionName(trim(form.getInstitutionName()));
        form.setHospitalLevel(trim(form.getHospitalLevel()));
        form.setInstitutionType(trim(form.getInstitutionType()));
        form.setPostcode(trim(form.getPostcode()));
        form.setLegalRepresentativeName(trim(form.getLegalRepresentativeName()));
        form.setLegalRepresentativeMobile(trim(form.getLegalRepresentativeMobile()));
        form.setContactName(trim(form.getContactName()));
        form.setContactPhone(trim(form.getContactPhone()));
        form.setContactMobile(trim(form.getContactMobile()));
        form.setAddress(trim(form.getAddress()));
        form.setNotes(trim(form.getNotes()));
         if (creating && isBlank(form.getInstitutionId())) {
            throw new InstitutionBusinessException("定点医疗机构编号不能为空");
        }
         if (isBlank(form.getInstitutionName())) {
            throw new InstitutionBusinessException("服务机构名称不能为空");
        }
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "定点医疗机构信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
