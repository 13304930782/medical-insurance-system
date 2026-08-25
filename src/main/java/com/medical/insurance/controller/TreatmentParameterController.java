package com.medical.insurance.controller;

import com.medical.insurance.exception.TreatmentParameterBusinessException;
import com.medical.insurance.model.CappingLineForm;
import com.medical.insurance.model.MinimumPaymentStandardForm;
import com.medical.insurance.model.SegmentRatioForm;
import com.medical.insurance.service.impl.TreatmentParameterService;

import java.math.BigDecimal;
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
@RequestMapping("/api/treatment-parameters")
public class TreatmentParameterController {
    private final TreatmentParameterService service;

    TreatmentParameterController(TreatmentParameterService service) { this.service = service; }

    @GetMapping("/capping-lines") Map<String,Object> cappingLines(@RequestParam(defaultValue="") String keyword) { return success(service.cappingLines(keyword)); }
    @PostMapping("/capping-lines") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createCapping(@RequestBody CappingLineForm form, HttpServletRequest request) { service.createCapping(form, request); return success("新增成功"); }
    @PutMapping("/capping-lines/{category}") Map<String,Object> updateCapping(@PathVariable String category, @RequestBody CappingLineForm form, HttpServletRequest request) { service.updateCapping(category, form, request); return success("保存成功"); }
    @DeleteMapping("/capping-lines/{category}") Map<String,Object> deleteCapping(@PathVariable String category, HttpServletRequest request) { service.deleteCapping(category, request); return success("删除成功"); }

    @GetMapping("/minimum-payment-standards") Map<String,Object> minimumStandards(@RequestParam(defaultValue="") String keyword) { return success(service.minimumStandards(keyword)); }
    @PostMapping("/minimum-payment-standards") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createMinimum(@RequestBody MinimumPaymentStandardForm form, HttpServletRequest request) { service.createMinimum(form, request); return success("新增成功"); }
    @PutMapping("/minimum-payment-standards") Map<String,Object> updateMinimum(@RequestBody MinimumPaymentStandardForm form, HttpServletRequest request) { service.updateMinimum(form, request); return success("保存成功"); }
    @DeleteMapping("/minimum-payment-standards") Map<String,Object> deleteMinimum(@RequestParam String medicalCategory, @RequestParam String medicalPersonnelCategory, @RequestParam String hospitalLevel, HttpServletRequest request) { service.deleteMinimum(medicalCategory, medicalPersonnelCategory, hospitalLevel, request); return success("删除成功"); }

    @GetMapping("/segment-ratios") Map<String,Object> segmentRatios(@RequestParam(defaultValue="") String keyword) { return success(service.segmentRatios(keyword)); }
    @PostMapping("/segment-ratios") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createSegment(@RequestBody SegmentRatioForm form, HttpServletRequest request) { service.createSegment(form, request); return success("新增成功"); }
    @PutMapping("/segment-ratios") Map<String,Object> updateSegment(@RequestBody SegmentRatioForm form, HttpServletRequest request) { service.updateSegment(form, request); return success("保存成功"); }
    @DeleteMapping("/segment-ratios") Map<String,Object> deleteSegment(@RequestParam String medicalCategory, @RequestParam String medicalPersonnelCategory, @RequestParam String hospitalLevel, @RequestParam BigDecimal minimumAmount, @RequestParam BigDecimal maximumAmount, HttpServletRequest request) { SegmentRatioForm form = new SegmentRatioForm(); form.setMedicalCategory(medicalCategory); form.setMedicalPersonnelCategory(medicalPersonnelCategory); form.setHospitalLevel(hospitalLevel); form.setMinimumAmount(minimumAmount); form.setMaximumAmount(maximumAmount); form.setReimbursementProportion(BigDecimal.ZERO); service.deleteSegment(form, request); return success("删除成功"); }

    @ExceptionHandler(TreatmentParameterBusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> businessError(TreatmentParameterBusinessException exception) { Map<String,Object> response = new LinkedHashMap<>(); response.put("success",false); response.put("message",exception.getMessage()); return response; }

    private Map<String,Object> success(Object data) { Map<String,Object> response = new LinkedHashMap<>(); response.put("success",true); response.put("data",data); return response; }
}
