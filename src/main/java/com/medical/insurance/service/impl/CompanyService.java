package com.medical.insurance.service.impl;

import com.medical.insurance.dao.CompanyMapper;
import com.medical.insurance.exception.CompanyBusinessException;
import com.medical.insurance.model.CompanyForm;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public CompanyService(CompanyMapper companyMapper, AuthService authService, SystemMapper systemMapper) {
        this.companyMapper = companyMapper;
        this.authService = authService;
        this.systemMapper = systemMapper;
    }

    public Map<String, Object> page(String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), 5000);
        String normalizedKeyword = normalizeKeyword(keyword);
        long total = companyMapper.count(normalizedKeyword);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", companyMapper.findPage(normalizedKeyword, (page - 1) * size, size));
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> detail(String companyId) {
        Map<String, Object> company = companyMapper.findById(companyId);
        if (company == null) throw new CompanyBusinessException("未找到该单位");
        return company;
    }

    @Transactional
    public void create(CompanyForm form, HttpServletRequest request) {
        normalizeAndValidate(form, true);
         if (companyMapper.exists(form.getCompanyId()) > 0) {
            throw new CompanyBusinessException("单位编号已存在");
        }
        companyMapper.insert(form);
        record(authService.currentUserId(request), "CREATE", form.getCompanyId(), "新增单位：" + form.getCompanyName(), request);
    }

    @Transactional
    public void update(String companyId, CompanyForm form, HttpServletRequest request) {
        form.setCompanyId(companyId);
        normalizeAndValidate(form, false);
         if (companyMapper.update(companyId, form) == 0) {
            throw new CompanyBusinessException("未找到该单位");
        }
        record(authService.currentUserId(request), "UPDATE", companyId, "修改单位：" + form.getCompanyName(), request);
    }

    @Transactional
    public void delete(String companyId, HttpServletRequest request) {
        Map<String, Object> company = detail(companyId);
         if (companyMapper.delete(companyId) == 0) {
            throw new CompanyBusinessException("未找到该单位");
        }
        record(authService.currentUserId(request), "DELETE", companyId, "删除单位：" + company.get("companyName"), request);
    }

    private void normalizeAndValidate(CompanyForm form, boolean creating) {
        form.setCompanyId(trim(form.getCompanyId()));
        form.setCompanyName(trim(form.getCompanyName()));
        form.setCompanyType(trim(form.getCompanyType()));
        form.setAddress(trim(form.getAddress()));
        form.setPostcode(trim(form.getPostcode()));
        form.setPhoneNumber(trim(form.getPhoneNumber()));
        if (creating && isBlank(form.getCompanyId())) throw new CompanyBusinessException("单位编号不能为空");
        if (isBlank(form.getCompanyName())) throw new CompanyBusinessException("单位名称不能为空");
    }

    private void record(long userId, String type, String businessNo, String content, HttpServletRequest request) {
        systemMapper.recordOperation(userId, "单位信息维护", type, businessNo, content, "SUCCESS", request.getRemoteAddr());
    }

    private String normalizeKeyword(String value) { return isBlank(value) ? null : value.trim(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
