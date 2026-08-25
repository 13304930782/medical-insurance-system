package com.medical.insurance.config;

import com.medical.insurance.dao.UserMapper;
import com.medical.insurance.model.SystemUser;
import com.medical.insurance.service.impl.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserMapper userMapper;
    private final AuthService authService;
    private final String username;
    private final String password;
    private final String realName;
    private final String email;

    AdminBootstrapRunner(
        UserMapper userMapper,
        AuthService authService,
        @Value("${app.bootstrap-admin.username}") String username,
        @Value("${app.bootstrap-admin.password}") String password,
        @Value("${app.bootstrap-admin.real-name}") String realName,
        @Value("${app.bootstrap-admin.email:admin@medical.local}") String email
    ) {
        this.userMapper = userMapper;
        this.authService = authService;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.email=email;
    }

    @Override
    public void run(String... args) {
        SystemUser user=userMapper.findByUsername(username);
        if(user==null){userMapper.insert(username,authService.passwordEncoder().encode(password),realName,"ADMIN");user=userMapper.findByUsername(username);log.info("已初始化管理员账号：{}，首次登录后请修改默认密码",username);}
        userMapper.ensureSecurityEmail(user.getUserId(),email);
        userMapper.ensureApprovedState(user.getUserId());
    }
}
