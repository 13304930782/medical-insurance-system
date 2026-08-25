package com.medical.insurance.service.impl;

import com.medical.insurance.dao.UserMapper;
import com.medical.insurance.exception.AuthenticationException;
import com.medical.insurance.exception.AuthValidationException;
import com.medical.insurance.model.PasswordResetMailRequested;
import com.medical.insurance.model.SystemUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.medical.insurance.dao.SystemMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final String SESSION_USER_ID = "LOGIN_USER_ID";
    static final String SESSION_USERNAME = "LOGIN_USERNAME";
    static final String SESSION_REAL_NAME = "LOGIN_REAL_NAME";
    static final String SESSION_ROLE_CODE = "LOGIN_ROLE_CODE";
    static final String SESSION_VERSION = "LOGIN_SESSION_VERSION";

    private final UserMapper userMapper;
    private final SystemMapper systemMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom=new SecureRandom();
    private final String resetSecret;
    private final boolean exposeResetCode;
    private final ApplicationEventPublisher eventPublisher;
    private static final Pattern USERNAME=Pattern.compile("[A-Za-z0-9_]{4,30}");
    private static final Pattern EMAIL=Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    AuthService(UserMapper userMapper,SystemMapper systemMapper,ApplicationEventPublisher eventPublisher,
                @Value("${app.auth.reset-secret:local-development-reset-secret-change-me}")String resetSecret,
                 @Value("${app.auth.expose-reset-code:false}")boolean exposeResetCode) {
        this.userMapper = userMapper;
        this.systemMapper = systemMapper;
        this.eventPublisher=eventPublisher;
        this.resetSecret=resetSecret;
        this.exposeResetCode=exposeResetCode;
    }

    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        String loginIdentifier = username == null ? "" : username.trim();
        SystemUser user = userMapper.findByLoginIdentifier(loginIdentifier);
         if(user!=null){userMapper.insertSecurity(user.getUserId(),null);Map<String,Object> security=userMapper.security(user.getUserId());Object locked=security==null?null:security.get("lockedUntil");if(locked!=null&&LocalDateTime.parse(locked.toString().replace(' ','T')).isAfter(LocalDateTime.now()))throw new AuthenticationException("登录失败次数过多，账号已临时锁定15分钟");}
        Map<String,Object> accountState=user==null?null:userMapper.accountState(user.getUserId());
         if(user!=null&&accountState==null){userMapper.ensureApprovedState(user.getUserId());accountState=userMapper.accountState(user.getUserId());}
        String accountStatus=accountState==null?"":String.valueOf(accountState.get("accountStatus"));
        if(user!=null&&"PENDING".equals(accountStatus))throw new AuthenticationException("账号正在等待管理员审核");
        if(user!=null&&"REJECTED".equals(accountStatus))throw new AuthenticationException("账号注册申请未通过，请联系管理员");
         if (user == null || !user.isEnabled() || !"APPROVED".equals(accountStatus) || !passwordEncoder.matches(password == null ? "" : password, user.getPasswordHash())) {
            if(user!=null)userMapper.recordLoginFailure(user.getUserId());
            systemMapper.recordOperation(
                user == null ? null : user.getUserId(),
                "系统登录",
                "LOGIN",
                null,
                "用户名或邮箱登录失败：" + loginIdentifier,
                "FAILURE",
                clientIp(request)
            );
            throw new AuthenticationException("账号或密码错误");
        }

        userMapper.clearLoginFailures(user.getUserId());
        HttpSession oldSession=request.getSession(false);if(oldSession!=null)oldSession.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, user.getUserId());
        session.setAttribute(SESSION_USERNAME, user.getUsername());
        session.setAttribute(SESSION_REAL_NAME, user.getRealName());
        session.setAttribute(SESSION_ROLE_CODE, user.getRoleCode());
        session.setAttribute(SESSION_VERSION,((Number)accountState.get("sessionVersion")).longValue());
        establishSecurityContext(user.getUserId(),user.getUsername(),user.getRoleCode());
        systemMapper.recordOperation(user.getUserId(), "系统登录", "LOGIN", null, "账号登录成功", "SUCCESS", clientIp(request));
        return userView(user.getUserId(), user.getUsername(), user.getRealName(), user.getRoleCode());
    }

    @Transactional
    public Map<String,Object> register(String username,String realName,String email,String password,HttpServletRequest request){
        String account=normalize(username),name=normalize(realName),mail=normalize(email).toLowerCase();
        if(!USERNAME.matcher(account).matches())throw new AuthValidationException("账号只能包含字母、数字和下划线，长度4至30位");
        if(name.length()<2||name.length()>50)throw new AuthValidationException("姓名长度应为2至50位");
        if(!EMAIL.matcher(mail).matches()||mail.length()>120)throw new AuthValidationException("邮箱格式不正确");
        validatePassword(password);
        if(userMapper.findByUsername(account)!=null)throw new AuthValidationException("登录账号已经存在");
        userMapper.insertPending(account,passwordEncoder.encode(password),name);
        SystemUser created=userMapper.findByUsername(account);
         try{userMapper.insertSecurityWithEmail(created.getUserId(),mail);}catch(Exception exception){throw new AuthValidationException("该邮箱已被其他账号使用");}
        userMapper.insertPendingState(created.getUserId());
        systemMapper.recordOperation(created.getUserId(),"用户注册","REGISTER",null,"用户自主注册","SUCCESS",clientIp(request));
        Map<String,Object> result=userView(created.getUserId(),created.getUsername(),created.getRealName(),created.getRoleCode());result.put("accountStatus","PENDING");result.put("message","注册申请已提交，请等待管理员审核");return result;
    }

    @Transactional
    public Map<String,Object> requestPasswordReset(String username,String email,HttpServletRequest request){
        SystemUser user=userMapper.findByUsernameAndEmail(normalize(username),normalize(email));
        Map<String,Object> result=new LinkedHashMap<>();result.put("message","如果账号和邮箱匹配，重置验证码已生成，10分钟内有效");result.put("retryAfterSeconds",60);
        if(user==null||!approved(user.getUserId()))return result;
        userMapper.lockPasswordReset(user.getUserId());
        Integer retryAfter=userMapper.passwordResetRetryAfterSeconds(user.getUserId());
        if(retryAfter!=null&&retryAfter>0)return result;
        String code=String.format("%06d",secureRandom.nextInt(1_000_000));
        userMapper.invalidateResetTokens(user.getUserId());
        userMapper.insertResetToken(user.getUserId(),tokenHash(user.getUserId(),code),LocalDateTime.now().plusMinutes(10));
        eventPublisher.publishEvent(new PasswordResetMailRequested(normalize(email),code));
        systemMapper.recordOperation(user.getUserId(),"密码找回","RESET_REQUEST",null,"申请密码重置验证码","SUCCESS",clientIp(request));
        if(exposeResetCode)result.put("developmentResetCode",code);
        return result;
    }

    @Transactional
    public void resetPassword(String username,String email,String code,String password,HttpServletRequest request){
        validatePassword(password);SystemUser user=userMapper.findByUsernameAndEmail(normalize(username),normalize(email));
        if(user==null)throw new AuthValidationException("验证码错误或已失效");
        Map<String,Object> token=userMapper.latestResetToken(user.getUserId());
        if(token==null||token.get("expiresAt")==null||LocalDateTime.parse(token.get("expiresAt").toString().replace(' ','T')).isBefore(LocalDateTime.now()))throw new AuthValidationException("验证码错误或已失效");
        long tokenId=((Number)token.get("tokenId")).longValue();
        String expected=String.valueOf(token.get("tokenHash")),actual=tokenHash(user.getUserId(),normalize(code));
         if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.getBytes(StandardCharsets.UTF_8))){userMapper.recordResetFailure(tokenId);throw new AuthValidationException("验证码错误或已失效");}
        userMapper.updatePassword(user.getUserId(),passwordEncoder.encode(password));userMapper.recordPasswordChanged(user.getUserId());userMapper.bumpSessionVersion(user.getUserId());userMapper.consumeResetToken(tokenId);
        systemMapper.recordOperation(user.getUserId(),"密码找回","RESET_PASSWORD",null,"通过验证码重置密码","SUCCESS",clientIp(request));
    }

    @Transactional
    public void changePassword(String currentPassword,String newPassword,HttpServletRequest request){
        long userId=currentUserId(request);SystemUser user=userMapper.findById(userId);
        if(user==null||!passwordEncoder.matches(currentPassword==null?"":currentPassword,user.getPasswordHash()))throw new AuthValidationException("当前密码错误");
        validatePassword(newPassword);if(passwordEncoder.matches(newPassword,user.getPasswordHash()))throw new AuthValidationException("新密码不能与当前密码相同");
        userMapper.updatePassword(userId,passwordEncoder.encode(newPassword));userMapper.insertSecurity(userId,null);userMapper.recordPasswordChanged(userId);userMapper.bumpSessionVersion(userId);userMapper.invalidateResetTokens(userId);
        systemMapper.recordOperation(userId,"账号安全","CHANGE_PASSWORD",null,"用户修改登录密码","SUCCESS",clientIp(request));
        request.getSession(false).invalidate();
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
         if (session == null) {
            return;
        }
        Object userId = session.getAttribute(SESSION_USER_ID);
         if (userId instanceof Long) {
            systemMapper.recordOperation((Long) userId, "系统登录", "LOGOUT", null, "账号退出登录", "SUCCESS", clientIp(request));
        }
        session.invalidate();
        SecurityContextHolder.clearContext();
    }

    public Map<String, Object> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
         if (session == null || session.getAttribute(SESSION_USER_ID) == null) {
            throw new AuthenticationException("请先登录");
        }
        return userView(
            (Long) session.getAttribute(SESSION_USER_ID),
            (String) session.getAttribute(SESSION_USERNAME),
            (String) session.getAttribute(SESSION_REAL_NAME),
            (String) session.getAttribute(SESSION_ROLE_CODE)
        );
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    private void validatePassword(String password){if(password==null||password.length()<8||password.length()>64||!password.matches(".*[A-Z].*")||!password.matches(".*[a-z].*")||!password.matches(".*\\d.*")||!password.matches(".*[^A-Za-z0-9].*"))throw new AuthValidationException("密码须为8至64位，并包含大小写字母、数字和特殊字符");if(password.chars().anyMatch(value->value<33||value>126))throw new AuthValidationException("密码只能使用可见英文字符，不能包含空格或控制字符");}
    private String normalize(String value){return value==null?"":value.trim();}
    private String tokenHash(long userId,String code){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(resetSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));byte[] bytes=mac.doFinal((userId+":"+code).getBytes(StandardCharsets.UTF_8));StringBuilder text=new StringBuilder();for(byte value:bytes)text.append(String.format("%02x",value));return text.toString();}catch(Exception exception){throw new IllegalStateException("无法生成重置验证码摘要",exception);}}

    public long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
         if (session == null || !(session.getAttribute(SESSION_USER_ID) instanceof Long)) {
            throw new AuthenticationException("请先登录");
        }
        return (Long) session.getAttribute(SESSION_USER_ID);
    }

    public String currentRealName(HttpServletRequest request) {
        currentUserId(request);
        return (String) request.getSession(false).getAttribute(SESSION_REAL_NAME);
    }

    public String currentRoleCode(HttpServletRequest request) {
        currentUserId(request);
        return (String) request.getSession(false).getAttribute(SESSION_ROLE_CODE);
    }

    public boolean restoreSecurityContext(HttpServletRequest request){HttpSession session=request.getSession(false);if(session==null||!(session.getAttribute(SESSION_USER_ID) instanceof Long))return false;long userId=(Long)session.getAttribute(SESSION_USER_ID);Map<String,Object> state=userMapper.accountState(userId);Object version=session.getAttribute(SESSION_VERSION);if(state==null||!"APPROVED".equals(String.valueOf(state.get("accountStatus")))||!(version instanceof Number)||((Number)version).longValue()!=((Number)state.get("sessionVersion")).longValue()){session.invalidate();SecurityContextHolder.clearContext();return false;}establishSecurityContext(userId,String.valueOf(session.getAttribute(SESSION_USERNAME)),String.valueOf(session.getAttribute(SESSION_ROLE_CODE)));return true;}

    public List<Map<String,Object>> accounts(String status){String value=normalize(status).toUpperCase();if(!value.isEmpty()&&!Set.of("PENDING","APPROVED","REJECTED","DELETED").contains(value))throw new AuthValidationException("账号状态不正确");return userMapper.accounts(value.isEmpty()?null:value);}

    @Transactional
    public void approveAccount(long userId,String roleCode,HttpServletRequest request){String role=normalize(roleCode).toUpperCase();if(!Set.of("APPROVER","REIMBURSEMENT").contains(role))throw new AuthValidationException("注册账号只能分配审批员或报销经办员角色");long approver=currentUserId(request);if(userMapper.approveAccountState(userId,approver)!=1)throw new AuthValidationException("账号不存在或已经处理");if(userMapper.enableApprovedAccount(userId,role)!=1)throw new AuthValidationException("账号不存在或已经处理");systemMapper.recordOperation(approver,"账号审核","APPROVE",String.valueOf(userId),"审核通过，分配角色："+role,"SUCCESS",clientIp(request));}

    @Transactional
    public void rejectAccount(long userId,HttpServletRequest request){long approver=currentUserId(request);if(userMapper.rejectAccountState(userId,approver)!=1)throw new AuthValidationException("账号不存在或已经处理");systemMapper.recordOperation(approver,"账号审核","REJECT",String.valueOf(userId),"注册申请未通过","SUCCESS",clientIp(request));}

    @Transactional
    public void deleteAccount(long userId,HttpServletRequest request){long operator=currentUserId(request);if(operator==userId)throw new AuthValidationException("不能删除当前登录账号");SystemUser target=userMapper.findById(userId);if(target==null)throw new AuthValidationException("账号不存在");if("ADMIN".equals(target.getRoleCode()))throw new AuthValidationException("系统管理员账号不能删除");if(userMapper.softDeleteAccount(userId,operator)!=1)throw new AuthValidationException("账号已经删除");userMapper.setAccountEnabled(userId,false);userMapper.invalidateResetTokens(userId);systemMapper.recordOperation(operator,"账号管理","DELETE",String.valueOf(userId),"删除账号："+target.getUsername()+"（保留历史业务和审计记录）","SUCCESS",clientIp(request));}

    @Transactional
    public void restoreAccount(long userId,HttpServletRequest request){long operator=currentUserId(request);SystemUser target=userMapper.findById(userId);if(target==null)throw new AuthValidationException("账号不存在");if(userMapper.restoreDeletedAccount(userId)!=1)throw new AuthValidationException("账号未删除或已经恢复");Map<String,Object> state=userMapper.accountState(userId);userMapper.setAccountEnabled(userId,"APPROVED".equals(String.valueOf(state.get("accountStatus"))));systemMapper.recordOperation(operator,"账号管理","RESTORE",String.valueOf(userId),"恢复账号："+target.getUsername(),"SUCCESS",clientIp(request));}

    private Map<String, Object> userView(long userId, String username, String realName, String roleCode) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", userId);
        user.put("username", username);
        user.put("realName", realName);
        user.put("roleCode", roleCode);
        return user;
    }

    private boolean approved(long userId){Map<String,Object> state=userMapper.accountState(userId);return state!=null&&"APPROVED".equals(String.valueOf(state.get("accountStatus")));}
    private void establishSecurityContext(long userId,String username,String roleCode){Map<String,Object> principal=new LinkedHashMap<>();principal.put("userId",userId);principal.put("username",username);principal.put("roleCode",roleCode);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+roleCode))));}

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
         if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
