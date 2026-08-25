package com.medical.insurance.controller;

import com.medical.insurance.exception.DiagnosisBusinessException;
import com.medical.insurance.model.DiagnosisForm;
import com.medical.insurance.service.impl.DiagnosisService;

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
@RequestMapping("/api/diagnoses")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @GetMapping
    Map<String, Object> page(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return success(diagnosisService.page(keyword, page, size));
    }

    @GetMapping("/{diaId}")
    Map<String, Object> detail(@PathVariable String diaId) {
        return success(diagnosisService.detail(diaId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody DiagnosisForm form, HttpServletRequest request) {
        diagnosisService.create(form, request);
        return success("新增成功");
    }

    @PutMapping("/{diaId}")
    Map<String, Object> update(@PathVariable String diaId, @RequestBody DiagnosisForm form, HttpServletRequest request) {
        diagnosisService.update(diaId, form, request);
        return success("保存成功");
    }

    @DeleteMapping("/{diaId}")
    Map<String, Object> delete(@PathVariable String diaId, HttpServletRequest request) {
        diagnosisService.delete(diaId, request);
        return success("删除成功");
    }

    @ExceptionHandler(DiagnosisBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> businessError(DiagnosisBusinessException exception) {
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
