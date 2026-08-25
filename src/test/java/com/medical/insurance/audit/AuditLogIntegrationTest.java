package com.medical.insurance.audit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AuditLogIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    void businessOperationsAreAuditedButOrdinaryQueriesAreNotAndAdminCanExportXlsx()throws Exception{
        String username="audit_admin_"+System.nanoTime(),password="Admin#1234";
        jdbcTemplate.update("INSERT INTO sys_user(username,password_hash,real_name,role_code,enabled) VALUES(?,?,?,?,1)",username,new BCryptPasswordEncoder().encode(password),"日志测试管理员","ADMIN");
        long userId=jdbcTemplate.queryForObject("SELECT user_id FROM sys_user WHERE username=?",Long.class,username);
        jdbcTemplate.update("INSERT INTO ext_user_security(user_id,email) VALUES(?,?)",userId,username+"@example.test");
        jdbcTemplate.update("INSERT INTO ext_user_account_state(user_id,account_status,session_version,approved_at) VALUES(?,'APPROVED',1,NOW())",userId);
        long httpRowsBefore=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_operation_log WHERE operation_module='HTTP接口审计'",Long.class);
        MvcResult login=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("username",username,"password",password))))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(header().exists("X-Request-Id"));
        long httpRowsAfter=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_operation_log WHERE operation_module='HTTP接口审计'",Long.class);
        org.junit.jupiter.api.Assertions.assertEquals(httpRowsBefore,httpRowsAfter);
        mockMvc.perform(get("/api/audit-logs").session(session).param("keyword",username))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total",greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.items[0].operationLabel",is("登录系统")));
        mockMvc.perform(get("/api/audit-logs").session(session).param("keyword","/api/health"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total",is(0)));
        mockMvc.perform(get("/api/audit-logs/export.xlsx").session(session))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
