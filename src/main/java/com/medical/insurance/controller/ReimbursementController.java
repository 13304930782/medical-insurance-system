package com.medical.insurance.controller;

import com.medical.insurance.exception.ReimbursementBusinessException;
import com.medical.insurance.model.PrescriptionForm;
import com.medical.insurance.model.VisitForm;
import com.medical.insurance.service.impl.ReimbursementService;

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
@RequestMapping("/api/reimbursements/visits")
public class ReimbursementController {
    private final ReimbursementService service;
    ReimbursementController(ReimbursementService service){this.service=service;}
    @GetMapping Map<String,Object> visits(@RequestParam(defaultValue="")String keyword){return success(service.visits(keyword));}
    @GetMapping("/outpatient-candidate") Map<String,Object> outpatientCandidate(@RequestParam String personId){return success(service.reusableOutpatientVisit(personId));}
    @GetMapping("/{number}") Map<String,Object> visit(@PathVariable String number){return success(service.visit(number));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createVisit(@RequestBody VisitForm form,HttpServletRequest request){return success(service.createVisit(form,request));}
    @PutMapping("/{number}") Map<String,Object> updateVisit(@PathVariable String number,@RequestBody VisitForm form,HttpServletRequest request){service.updateVisit(number,form,request);return success("保存成功");}
    @DeleteMapping("/{number}") Map<String,Object> deleteVisit(@PathVariable String number,HttpServletRequest request){service.deleteVisit(number,request);return success("删除成功");}
    @GetMapping("/{number}/prescriptions") Map<String,Object> prescriptions(@PathVariable String number){return success(service.prescriptions(number));}
    @PostMapping("/{number}/prescriptions") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createPrescription(@PathVariable String number,@RequestBody PrescriptionForm form,HttpServletRequest request){return success(service.createPrescription(number,form,request));}
    @PutMapping("/{number}/prescriptions") Map<String,Object> updatePrescription(@PathVariable String number,@RequestBody PrescriptionForm form,HttpServletRequest request){service.updatePrescription(number,form,request);return success("保存成功");}
    @DeleteMapping("/{number}/prescriptions") Map<String,Object> deletePrescription(@PathVariable String number,@RequestBody PrescriptionForm form,HttpServletRequest request){service.deletePrescription(number,form,request);return success("删除成功");}
    @ExceptionHandler(ReimbursementBusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> businessError(ReimbursementBusinessException e){Map<String,Object> r=new LinkedHashMap<>();r.put("success",false);r.put("message",e.getMessage());return r;}
    private Map<String,Object> success(Object data){Map<String,Object> r=new LinkedHashMap<>();r.put("success",true);r.put("data",data);return r;}
}
