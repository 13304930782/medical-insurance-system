package com.medical.insurance.controller;

import com.medical.insurance.exception.DiseaseBusinessException;
import com.medical.insurance.model.DiseaseForm;
import com.medical.insurance.service.impl.DiseaseService;

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
@RequestMapping("/api/diseases")
public class DiseaseController {

    private final DiseaseService diseaseService;

    DiseaseController(DiseaseService diseaseService) {
        this.diseaseService = diseaseService;
    }

    @GetMapping
    Map<String, Object> page(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return success(diseaseService.page(keyword, page, size));
    }

    @GetMapping("/{diseaseId}")
    Map<String, Object> detail(@PathVariable String diseaseId) {
        return success(diseaseService.detail(diseaseId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody DiseaseForm form, HttpServletRequest request) {
        diseaseService.create(form, request);
        return success("新增成功");
    }

    @PutMapping("/{diseaseId}")
    Map<String, Object> update(@PathVariable String diseaseId, @RequestBody DiseaseForm form, HttpServletRequest request) {
        diseaseService.update(diseaseId, form, request);
        return success("保存成功");
    }

    @DeleteMapping("/{diseaseId}")
    Map<String, Object> delete(@PathVariable String diseaseId, HttpServletRequest request) {
        diseaseService.delete(diseaseId, request);
        return success("删除成功");
    }

    @ExceptionHandler(DiseaseBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> businessError(DiseaseBusinessException exception) {
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
