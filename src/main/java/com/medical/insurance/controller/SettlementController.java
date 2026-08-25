package com.medical.insurance.controller;

import com.medical.insurance.exception.ReimbursementBusinessException;
import com.medical.insurance.service.impl.SettlementService;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reimbursements")
public class SettlementController {
    private final SettlementService service;
    SettlementController(SettlementService service){this.service=service;}
    @PostMapping("/visits/{number}/preview") Map<String,Object> preview(@PathVariable String number){return success(service.preview(number));}
    @PostMapping("/visits/{number}/settle") Map<String,Object> settle(@PathVariable String number,HttpServletRequest request){return success(service.settle(number,request));}
    @GetMapping("/settlements") Map<String,Object> settlements(@RequestParam(defaultValue="")String keyword,@RequestParam(required=false)Integer year){return success(service.settlements(keyword,year));}
    @GetMapping("/settlements/{id}") Map<String,Object> settlement(@PathVariable Long id){return success(service.settlement(id));}
    @PostMapping("/settlements/{id}/cancel") Map<String,Object> cancel(@PathVariable Long id,@RequestBody Map<String,Object> body,HttpServletRequest request){return success(service.cancel(id,body.get("reason")==null?null:String.valueOf(body.get("reason")),request));}
    @ExceptionHandler(ReimbursementBusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> error(ReimbursementBusinessException e){Map<String,Object> r=new LinkedHashMap<>();r.put("success",false);r.put("message",e.getMessage());return r;}
    private Map<String,Object> success(Object data){Map<String,Object> r=new LinkedHashMap<>();r.put("success",true);r.put("data",data);return r;}
}
