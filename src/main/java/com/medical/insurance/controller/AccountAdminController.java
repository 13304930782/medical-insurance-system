package com.medical.insurance.controller;

import com.medical.insurance.exception.AuthValidationException;
import com.medical.insurance.service.impl.AuthService;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountAdminController {
    private final AuthService authService;

    AccountAdminController(AuthService authService){this.authService=authService;}

    @GetMapping
    Map<String,Object> accounts(@RequestParam(required=false)String status){return success(authService.accounts(status));}

    @PostMapping("/{userId}/approve")
    Map<String,Object> approve(@PathVariable long userId,@RequestBody RoleRequest body,HttpServletRequest request){authService.approveAccount(userId,body.roleCode,request);return success("账号审核通过");}

    @PostMapping("/{userId}/reject")
    Map<String,Object> reject(@PathVariable long userId,HttpServletRequest request){authService.rejectAccount(userId,request);return success("账号申请已拒绝");}

    @DeleteMapping("/{userId}")
    Map<String,Object> delete(@PathVariable long userId,HttpServletRequest request){authService.deleteAccount(userId,request);return success("账号已删除");}

    @PostMapping("/{userId}/restore")
    Map<String,Object> restore(@PathVariable long userId,HttpServletRequest request){authService.restoreAccount(userId,request);return success("账号已恢复");}

    @ExceptionHandler(AuthValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> validation(AuthValidationException exception){Map<String,Object> result=new LinkedHashMap<>();result.put("success",false);result.put("message",exception.getMessage());return result;}

    private Map<String,Object> success(Object data){Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("data",data);return result;}
    static final class RoleRequest{public String roleCode;}
}
