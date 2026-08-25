package com.medical.insurance.config;

import com.medical.insurance.dao.UserMapper;
import com.medical.insurance.model.SystemUser;
import com.medical.insurance.service.impl.AuthService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(11)
public class RoleUserBootstrapRunner implements CommandLineRunner {
    private final UserMapper mapper;private final AuthService authService;private final String password;
    RoleUserBootstrapRunner(UserMapper mapper,AuthService authService,@Value("${app.bootstrap-role-users.password}")String password){this.mapper=mapper;this.authService=authService;this.password=password;}
    @Override public void run(String... args){create("approver","待遇审批员","APPROVER");create("reimbursement","报销经办员","REIMBURSEMENT");}
    private void create(String username,String realName,String role){SystemUser user=mapper.findByUsername(username);if(user==null){mapper.insert(username,authService.passwordEncoder().encode(password),realName,role);user=mapper.findByUsername(username);}mapper.ensureSecurityEmail(user.getUserId(),username+"@medical.local");mapper.ensureApprovedState(user.getUserId());}
}
