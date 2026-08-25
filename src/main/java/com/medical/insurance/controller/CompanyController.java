package com.medical.insurance.controller;

import com.medical.insurance.exception.CompanyBusinessException;
import com.medical.insurance.model.CompanyForm;
import com.medical.insurance.service.impl.CompanyService;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    Map<String, Object> page(@RequestParam(defaultValue = "") String keyword,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return success(companyService.page(keyword, page, size));
    }

    @GetMapping("/{companyId}")
    Map<String, Object> detail(@PathVariable String companyId) {
        return success(companyService.detail(companyId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody CompanyForm form, HttpServletRequest request) {
        companyService.create(form, request);
        return success("新增成功");
    }

    @PutMapping("/{companyId}")
    Map<String, Object> update(@PathVariable String companyId, @RequestBody CompanyForm form, HttpServletRequest request) {
        companyService.update(companyId, form, request);
        return success("保存成功");
    }

    @DeleteMapping("/{companyId}")
    Map<String, Object> delete(@PathVariable String companyId, HttpServletRequest request) {
        companyService.delete(companyId, request);
        return success("删除成功");
    }

    @ExceptionHandler(CompanyBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> businessError(CompanyBusinessException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", exception.getMessage());
        return response;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
}
