package com.medical.insurance.controller;

import com.medical.insurance.exception.AuthenticationException;
import com.medical.insurance.exception.AuthValidationException;
import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.service.impl.PasswordCipherService;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.dao.SystemMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final SystemMapper systemMapper;
    private final PasswordCipherService passwordCipherService;

    AuthController(AuthService authService,SystemMapper systemMapper,PasswordCipherService passwordCipherService) {
        this.authService = authService;
        this.systemMapper = systemMapper;
        this.passwordCipherService=passwordCipherService;
    }

    @GetMapping("/auth/encryption-challenge")
    Map<String,Object> encryptionChallenge(HttpServletRequest request){return success(passwordCipherService.challenge(request));}

    @PostMapping("/auth/login")
    Map<String, Object> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String password=passwordCipherService.decrypt(loginRequest.challengeId,loginRequest.encryptedPassword,loginRequest.password,request);
        return success(authService.login(loginRequest.getUsername(),password,request));
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> register(@RequestBody RegisterRequest body,HttpServletRequest request){String password=passwordCipherService.decrypt(body.challengeId,body.encryptedPassword,body.password,request);return success(authService.register(body.username,body.realName,body.email,password,request));}

    @PostMapping("/auth/password-reset/request")
    Map<String,Object> requestPasswordReset(@RequestBody ResetRequest body,HttpServletRequest request){return success(authService.requestPasswordReset(body.username,body.email,request));}

    @PostMapping("/auth/password-reset/confirm")
    Map<String,Object> resetPassword(@RequestBody ResetConfirmRequest body,HttpServletRequest request){String password=passwordCipherService.decrypt(body.challengeId,body.encryptedPassword,body.password,request);authService.resetPassword(body.username,body.email,body.code,password,request);return success("密码已重置，请使用新密码登录");}

    @PostMapping("/auth/change-password")
    Map<String,Object> changePassword(@RequestBody ChangePasswordRequest body,HttpServletRequest request){String payload=passwordCipherService.decrypt(body.challengeId,body.encryptedPassword,body.passwordPayload,request);int separator=payload.indexOf('\0');if(separator<1)throw new AuthValidationException("密码变更数据格式不正确");authService.changePassword(payload.substring(0,separator),payload.substring(separator+1),request);return success("密码修改成功，请重新登录");}

    @PostMapping("/auth/logout")
    Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request);
        return success("已退出登录");
    }

    @GetMapping("/auth/me")
    Map<String, Object> currentUser(HttpServletRequest request) {
        return success(authService.currentUser(request));
    }

    @GetMapping("/health")
    Map<String, Object> health() {
        Integer value = systemMapper.health();
        return success(value != null && value == 1 ? "数据库连接正常" : "数据库连接异常");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    Map<String, Object> authenticationError(AuthenticationException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", exception.getMessage());
        return response;
    }

    @ExceptionHandler(AuthValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> validationError(AuthValidationException exception){Map<String,Object> response=new LinkedHashMap<>();response.put("success",false);response.put("message",exception.getMessage());return response;}

    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }

    public static final class LoginRequest {
        private String username;
        private String password;
        private String challengeId;
        private String encryptedPassword;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getChallengeId(){return challengeId;}public void setChallengeId(String value){challengeId=value;}
        public String getEncryptedPassword(){return encryptedPassword;}public void setEncryptedPassword(String value){encryptedPassword=value;}
    }

    public static final class RegisterRequest {public String username;public String realName;public String email;public String password;public String challengeId;public String encryptedPassword;}
    public static class ResetRequest {public String username;public String email;}
    public static final class ResetConfirmRequest extends ResetRequest {public String code;public String password;public String challengeId;public String encryptedPassword;}
    public static final class ChangePasswordRequest {public String passwordPayload;public String challengeId;public String encryptedPassword;}
}
