package com.medical.insurance.service.impl;

import com.medical.insurance.dao.FacilityMapper;
import com.medical.insurance.exception.FacilityBusinessException;
import com.medical.insurance.model.FacilityForm;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityService {

    private final FacilityMapper facilityMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public FacilityService(FacilityMapper facilityMapper, AuthService authService, SystemMapper systemMapper) {
        this.facilityMapper = facilityMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        String normalizedKeyword = normalizeKeyword(keyword);
        long total = facilityMapper.count(normalizedKeyword);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", facilityMapper.findPage(normalizedKeyword, (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String serId) {
        Map<String, Object> facility = facilityMapper.findById(serId);
        if (facility == null) throw new FacilityBusinessException("未找到该服务设施");
        return facility;
    }

    @Transactional
    public void create(FacilityForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
         if (facilityMapper.exists(form.getSerId()) > 0) {
            throw new FacilityBusinessException("服务设施编码已存在");
        }
        facilityMapper.insert(form);
        record(authService.currentUserId(request), "CREATE", form.getSerId(), "新增服务设施：" + form.getSerName(), request);
    }

    @Transactional
    public void update(String serId, FacilityForm form, HttpServletRequest request) {
        form.setSerId(serId);
        normalizeAndValidate(form, false);
         if (facilityMapper.update(serId, form) == 0) {
            throw new FacilityBusinessException("未找到该服务设施");
        }
        record(authService.currentUserId(request), "UPDATE", serId, "修改服务设施：" + form.getSerName(), request);
    }

    @Transactional
    public void delete(String serId, HttpServletRequest request) {
        Map<String, Object> facility = detail(serId);
         if (facilityMapper.delete(serId) == 0) {
            throw new FacilityBusinessException("未找到该服务设施");
        }
        record(authService.currentUserId(request), "DELETE", serId, "删除服务设施：" + facility.get("serName"), request);
    }

    private void normalizeAndValidate(FacilityForm form, boolean creating) {
        form.setSerId(trim(form.getSerId()));
        form.setSerName(trim(form.getSerName()));
        form.setSerExpType(trim(form.getSerExpType()));
         if (creating && isBlank(form.getSerId())) {
            throw new FacilityBusinessException("服务设施编码不能为空");
        }
         if (isBlank(form.getSerName())) {
            throw new FacilityBusinessException("服务设施名称不能为空");
        }
        if (isBlank(form.getSerValid())) form.setSerValid("有效");
        form.setSerStarttime(normalizeDateTime(form.getSerStarttime()));
        form.setSerEndtime(normalizeDateTime(form.getSerEndtime()));
    }

    private String normalizeDateTime(String value) {
        if (isBlank(value)) return null;
        String normalized = value.trim().replace('T', ' ');
        return normalized.length() == 16 ? normalized + ":00" : normalized;
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "服务设施信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
