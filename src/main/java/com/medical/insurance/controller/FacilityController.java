package com.medical.insurance.controller;

import com.medical.insurance.exception.FacilityBusinessException;
import com.medical.insurance.model.FacilityForm;
import com.medical.insurance.service.impl.FacilityService;

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
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    Map<String, Object> page(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return success(facilityService.page(keyword, page, size));
    }

    @GetMapping("/{serId}")
    Map<String, Object> detail(@PathVariable String serId) {
        return success(facilityService.detail(serId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody FacilityForm form, HttpServletRequest request) {
        facilityService.create(form, request);
        return success("新增成功");
    }

    @PutMapping("/{serId}")
    Map<String, Object> update(@PathVariable String serId, @RequestBody FacilityForm form, HttpServletRequest request) {
        facilityService.update(serId, form, request);
        return success("保存成功");
    }

    @DeleteMapping("/{serId}")
    Map<String, Object> delete(@PathVariable String serId, HttpServletRequest request) {
        facilityService.delete(serId, request);
        return success("删除成功");
    }

    @ExceptionHandler(FacilityBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> businessError(FacilityBusinessException exception) {
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
