package com.medical.insurance.service.impl;

import com.medical.insurance.dao.DiseaseMapper;
import com.medical.insurance.exception.DiseaseBusinessException;
import com.medical.insurance.model.DiseaseForm;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiseaseService {

    private final DiseaseMapper diseaseMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public DiseaseService(DiseaseMapper diseaseMapper, AuthService authService, SystemMapper systemMapper) {
        this.diseaseMapper = diseaseMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        String normalizedKeyword = normalizeKeyword(keyword);
        long total = diseaseMapper.count(normalizedKeyword);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", diseaseMapper.findPage(normalizedKeyword, (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String diseaseId) {
        Map<String, Object> disease = diseaseMapper.findById(diseaseId);
        if (disease == null) throw new DiseaseBusinessException("未找到该病种");
        return disease;
    }

    @Transactional
    public void create(DiseaseForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
         if (diseaseMapper.exists(form.getDiseaseId()) > 0) {
            throw new DiseaseBusinessException("病种编码已存在");
        }
        diseaseMapper.insert(form);
        record(authService.currentUserId(request), "CREATE", form.getDiseaseId(), "新增病种：" + form.getDiseaseName(), request);
    }

    @Transactional
    public void update(String diseaseId, DiseaseForm form, HttpServletRequest request) {
        form.setDiseaseId(diseaseId);
        normalizeAndValidate(form, false);
         if (diseaseMapper.update(diseaseId, form) == 0) {
            throw new DiseaseBusinessException("未找到该病种");
        }
        record(authService.currentUserId(request), "UPDATE", diseaseId, "修改病种：" + form.getDiseaseName(), request);
    }

    @Transactional
    public void delete(String diseaseId, HttpServletRequest request) {
        Map<String, Object> disease = detail(diseaseId);
         if (diseaseMapper.delete(diseaseId) == 0) {
            throw new DiseaseBusinessException("未找到该病种");
        }
        record(authService.currentUserId(request), "DELETE", diseaseId, "删除病种：" + disease.get("diseaseName"), request);
    }

    private void normalizeAndValidate(DiseaseForm form, boolean creating) {
        form.setDiseaseId(trim(form.getDiseaseId()));
        form.setDiseaseName(trim(form.getDiseaseName()));
        form.setDiseaseType(trim(form.getDiseaseType()));
        form.setDiseaseReimbursementStandards(trim(form.getDiseaseReimbursementStandards()));
        form.setNotes(trim(form.getNotes()));
         if (creating && isBlank(form.getDiseaseId())) {
            throw new DiseaseBusinessException("病种编码不能为空");
        }
         if (isBlank(form.getDiseaseName())) {
            throw new DiseaseBusinessException("病种名称不能为空");
        }
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "病种信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
