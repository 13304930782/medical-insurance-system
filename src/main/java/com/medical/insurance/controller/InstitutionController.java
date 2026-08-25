package com.medical.insurance.controller;

import com.medical.insurance.exception.InstitutionBusinessException;
import com.medical.insurance.model.InstitutionForm;
import com.medical.insurance.service.impl.InstitutionService;

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
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    Map<String, Object> page(@RequestParam(defaultValue = "") String keyword,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return success(institutionService.page(keyword, page, size));
    }

    @GetMapping("/{institutionId}")
    Map<String, Object> detail(@PathVariable String institutionId) {
        return success(institutionService.detail(institutionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody InstitutionForm form, HttpServletRequest request) {
        institutionService.create(form, request);
        return success("新增成功");
    }

    @PutMapping("/{institutionId}")
    Map<String, Object> update(@PathVariable String institutionId, @RequestBody InstitutionForm form,
                               HttpServletRequest request) {
        institutionService.update(institutionId, form, request);
        return success("保存成功");
    }

    @DeleteMapping("/{institutionId}")
    Map<String, Object> delete(@PathVariable String institutionId, HttpServletRequest request) {
        institutionService.delete(institutionId, request);
        return success("删除成功");
    }

    @ExceptionHandler(InstitutionBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> businessError(InstitutionBusinessException exception) {
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
