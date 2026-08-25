package com.medical.insurance.service.impl;

import com.medical.insurance.dao.MedicineMapper;
import com.medical.insurance.exception.MedicineBusinessException;
import com.medical.insurance.model.MedicineForm;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicineService {

    private final MedicineMapper medicineMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public MedicineService(MedicineMapper medicineMapper, AuthService authService, SystemMapper systemMapper) {
        this.medicineMapper = medicineMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        long total = medicineMapper.count(normalizeKeyword(keyword));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", medicineMapper.findPage(normalizeKeyword(keyword), (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String medId) {
        Map<String, Object> medicine = medicineMapper.findById(medId);
        if (medicine == null) throw new MedicineBusinessException("未找到该药品");
        return medicine;
    }

    @Transactional
    public void create(MedicineForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
         if (medicineMapper.exists(form.getMedId()) > 0) {
            throw new MedicineBusinessException("药品编码已存在");
        }
        medicineMapper.insert(form);
        record(authService.currentUserId(request), "CREATE", form.getMedId(), "新增药品：" + form.getMedName(), request);
    }

    @Transactional
    public void update(String medId, MedicineForm form, HttpServletRequest request) {
        form.setMedId(medId);
        normalizeAndValidate(form, false);
         if (medicineMapper.update(medId, form) == 0) {
            throw new MedicineBusinessException("未找到该药品");
        }
        record(authService.currentUserId(request), "UPDATE", medId, "修改药品：" + form.getMedName(), request);
    }

    @Transactional
    public void delete(String medId, HttpServletRequest request) {
        Map<String, Object> medicine = detail(medId);
         if (medicineMapper.delete(medId) == 0) {
            throw new MedicineBusinessException("未找到该药品");
        }
        record(authService.currentUserId(request), "DELETE", medId, "删除药品：" + medicine.get("medName"), request);
    }

    private void normalizeAndValidate(MedicineForm form, boolean creating) {
        form.setMedId(trim(form.getMedId()));
        form.setMedName(trim(form.getMedName()));
         if (creating && (form.getMedId() == null || form.getMedId().isEmpty())) {
            throw new MedicineBusinessException("药品编码不能为空");
        }
         if (form.getMedName() == null || form.getMedName().isEmpty()) {
            throw new MedicineBusinessException("药品名称不能为空");
        }
        if (form.getMedMaxPrize() == null) form.setMedMaxPrize(BigDecimal.ZERO);
        if (isBlank(form.getMedApprovalmark())) form.setMedApprovalmark("不需要审批");
        if (requiresSpecialApproval(form.getMedSpecialmark())) form.setMedApprovalmark("需要审批");
        if (isBlank(form.getMedHosLevel())) form.setMedHosLevel("所有医院");
        if (isBlank(form.getMedValid())) form.setMedValid("有效");
        form.setMedStarttime(normalizeDateTime(form.getMedStarttime()));
        form.setMedEndtime(normalizeDateTime(form.getMedEndtime()));
    }

    private String normalizeDateTime(String value) {
        if (isBlank(value)) return null;
        String normalized = value.trim().replace('T', ' ');
        return normalized.length() == 16 ? normalized + ":00" : normalized;
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "药品信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private boolean requiresSpecialApproval(String value) { return !isBlank(value) && ("是".equals(value.trim()) || "特检特治".equals(value.trim()) || "需要审批".equals(value.trim()) || "1".equals(value.trim())); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
