package com.medical.insurance.controller;

import com.medical.insurance.exception.MedicineBusinessException;
import com.medical.insurance.model.MedicineForm;
import com.medical.insurance.service.impl.MedicineService;

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
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    Map<String, Object> page(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return success(medicineService.page(keyword, page, size));
    }

    @GetMapping("/{medId}")
    Map<String, Object> detail(@PathVariable String medId) {
        return success(medicineService.detail(medId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody MedicineForm form, HttpServletRequest request) {
        medicineService.create(form, request);
        return success("新增成功");
    }

    @PutMapping("/{medId}")
    Map<String, Object> update(@PathVariable String medId, @RequestBody MedicineForm form, HttpServletRequest request) {
        medicineService.update(medId, form, request);
        return success("保存成功");
    }

    @DeleteMapping("/{medId}")
    Map<String, Object> delete(@PathVariable String medId, HttpServletRequest request) {
        medicineService.delete(medId, request);
        return success("删除成功");
    }

    @ExceptionHandler(MedicineBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> businessError(MedicineBusinessException exception) {
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
