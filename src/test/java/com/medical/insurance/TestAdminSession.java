package com.medical.insurance;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAdminSession {
    private TestAdminSession() {}

    public static MockHttpSession create(MockMvc mockMvc, JdbcTemplate jdbc) throws Exception {
        String username="test_admin_"+UUID.randomUUID().toString().replace("-","");
        String password="Admin#1234";
        jdbc.update("INSERT INTO sys_user(username,password_hash,real_name,role_code,enabled) VALUES(?,?,?,?,1)",username,new BCryptPasswordEncoder().encode(password),"集成测试管理员","ADMIN");
        long userId=jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username=?",Long.class,username);
        jdbc.update("INSERT INTO ext_user_security(user_id,email) VALUES(?,?)",userId,username+"@example.test");
        jdbc.update("INSERT INTO ext_user_account_state(user_id,account_status,session_version,approved_at) VALUES(?,'APPROVED',1,NOW())",userId);
        MvcResult result=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"password\":\""+password+"\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
}
