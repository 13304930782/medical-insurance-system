package com.medical.insurance.service.impl;

import com.medical.insurance.dao.DiagnosisMapper;
import com.medical.insurance.exception.DiagnosisBusinessException;
import com.medical.insurance.model.DiagnosisForm;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiagnosisService {

    private final DiagnosisMapper diagnosisMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public DiagnosisService(DiagnosisMapper diagnosisMapper, AuthService authService, SystemMapper systemMapper) {
        this.diagnosisMapper = diagnosisMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        long total = diagnosisMapper.count(normalizeKeyword(keyword));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", diagnosisMapper.findPage(normalizeKeyword(keyword), (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String diaId) {
        Map<String, Object> diagnosis = diagnosisMapper.findById(diaId);
        if (diagnosis == null) throw new DiagnosisBusinessException("未找到该诊疗项目");
        return diagnosis;
    }

    @Transactional
    public void create(DiagnosisForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
         if (diagnosisMapper.exists(form.getDiaId()) > 0) {
            throw new DiagnosisBusinessException("诊疗项目编码已存在");
        }
        diagnosisMapper.insert(form);
        record(authService.currentUserId(request), "CREATE", form.getDiaId(), "新增诊疗项目：" + form.getDiaName(), request);
    }

    @Transactional
    public void update(String diaId, DiagnosisForm form, HttpServletRequest request) {
        form.setDiaId(diaId);
        normalizeAndValidate(form, false);
         if (diagnosisMapper.update(diaId, form) == 0) {
            throw new DiagnosisBusinessException("未找到该诊疗项目");
        }
        record(authService.currentUserId(request), "UPDATE", diaId, "修改诊疗项目：" + form.getDiaName(), request);
    }

    @Transactional
    public void delete(String diaId, HttpServletRequest request) {
        Map<String, Object> diagnosis = detail(diaId);
         if (diagnosisMapper.delete(diaId) == 0) {
            throw new DiagnosisBusinessException("未找到该诊疗项目");
        }
        record(authService.currentUserId(request), "DELETE", diaId, "删除诊疗项目：" + diagnosis.get("diaName"), request);
    }

    private void normalizeAndValidate(DiagnosisForm form, boolean creating) {
        form.setDiaId(trim(form.getDiaId()));
        form.setDiaName(trim(form.getDiaName()));
         if (creating && isBlank(form.getDiaId())) {
            throw new DiagnosisBusinessException("诊疗项目编码不能为空");
        }
         if (isBlank(form.getDiaName())) {
            throw new DiagnosisBusinessException("诊疗项目名称不能为空");
        }
        if (form.getDiaMaxPrize() == null) form.setDiaMaxPrize(BigDecimal.ZERO);
        if (isBlank(form.getDiaApprovalmark())) form.setDiaApprovalmark("不需要审批");
        if (requiresSpecialApproval(form.getDiaExpType())) form.setDiaApprovalmark("需要审批");
        if (isBlank(form.getDiaHosLevel())) form.setDiaHosLevel("所有医院");
        if (isBlank(form.getDiaValid())) form.setDiaValid("有效");
        form.setDiaStarttime(normalizeDateTime(form.getDiaStarttime()));
        form.setDiaEndtime(normalizeDateTime(form.getDiaEndtime()));
    }

    private String normalizeDateTime(String value) {
        if (isBlank(value)) return null;
        String normalized = value.trim().replace('T', ' ');
        return normalized.length() == 16 ? normalized + ":00" : normalized;
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "诊疗项目信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private boolean requiresSpecialApproval(String value) { return !isBlank(value) && ("特殊检查费".equals(value.trim()) || "特殊治疗费".equals(value.trim()) || "特检费".equals(value.trim()) || "特治费".equals(value.trim())); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
