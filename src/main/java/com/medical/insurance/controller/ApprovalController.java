package com.medical.insurance.controller;

import com.medical.insurance.exception.ApprovalBusinessException;
import com.medical.insurance.model.InstitutionApprovalForm;
import com.medical.insurance.model.SpecialApprovalForm;
import com.medical.insurance.service.impl.ApprovalService;

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
@RequestMapping("/api/approvals")
public class ApprovalController {
    private final ApprovalService service;
    ApprovalController(ApprovalService service){this.service=service;}

    @GetMapping("/institutions") Map<String,Object> institutions(@RequestParam(defaultValue="") String keyword){return success(service.institutionApprovals(keyword));}
    @PostMapping("/institutions") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createInstitution(@RequestBody InstitutionApprovalForm form,HttpServletRequest request){service.createInstitution(form,request);return success("新增成功");}
    @PutMapping("/institutions/{number}") Map<String,Object> updateInstitution(@PathVariable String number,@RequestBody InstitutionApprovalForm form,HttpServletRequest request){service.updateInstitution(number,form,request);return success("保存成功");}
    @DeleteMapping("/institutions/{number}") Map<String,Object> deleteInstitution(@PathVariable String number,HttpServletRequest request){service.deleteInstitution(number,request);return success("删除成功");}

    @GetMapping("/special") Map<String,Object> special(@RequestParam(defaultValue="") String keyword){return success(service.specialApprovals(keyword));}
    @GetMapping("/special/projects") Map<String,Object> specialProjects(@RequestParam String itemType,@RequestParam(defaultValue="") String keyword){return success(service.specialProjectOptions(itemType,keyword));}
    @PostMapping("/special") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createSpecial(@RequestBody SpecialApprovalForm form,HttpServletRequest request){service.createSpecial(form,request);return success("新增成功");}
    @PutMapping("/special/{number}") Map<String,Object> updateSpecial(@PathVariable String number,@RequestBody SpecialApprovalForm form,HttpServletRequest request){service.updateSpecial(number,form,request);return success("保存成功");}
    @DeleteMapping("/special/{number}") Map<String,Object> deleteSpecial(@PathVariable String number,HttpServletRequest request){service.deleteSpecial(number,request);return success("删除成功");}

    @ExceptionHandler(ApprovalBusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> businessError(ApprovalBusinessException exception){Map<String,Object> response=new LinkedHashMap<>();response.put("success",false);response.put("message",exception.getMessage());return response;}
    private Map<String,Object> success(Object data){Map<String,Object> response=new LinkedHashMap<>();response.put("success",true);response.put("data",data);return response;}
}
