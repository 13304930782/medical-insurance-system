package com.medical.insurance.auth;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AuthApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registrationLoginResetAndEncryptionChallengeWork() throws Exception {
        mockMvc.perform(get("/api/auth/encryption-challenge"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.algorithm",is("RSA-OAEP-256")));

        String register="{\"username\":\"secure_user\",\"realName\":\"安全用户\",\"email\":\"secure@example.test\",\"password\":\"Strong#123\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.accountStatus",is("PENDING")));
        String hash=jdbcTemplate.queryForObject("SELECT password_hash FROM sys_user WHERE username='secure_user'",String.class);
        assertFalse("Strong#123".equals(hash));assertTrue(hash.startsWith("$2"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(encryptedLogin("secure_user","Strong#123")))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message",is("账号正在等待管理员审核")));

        long userId=jdbcTemplate.queryForObject("SELECT user_id FROM sys_user WHERE username='secure_user'",Long.class);
        String testAdmin="auth_test_admin_"+System.nanoTime(),testAdminPassword="Admin#1234";
        jdbcTemplate.update("INSERT INTO sys_user(username,password_hash,real_name,role_code,enabled) VALUES(?,?,?,?,1)",testAdmin,new BCryptPasswordEncoder().encode(testAdminPassword),"认证测试管理员","ADMIN");
        long testAdminId=jdbcTemplate.queryForObject("SELECT user_id FROM sys_user WHERE username=?",Long.class,testAdmin);
        jdbcTemplate.update("INSERT INTO ext_user_security(user_id,email) VALUES(?,?)",testAdminId,testAdmin+"@example.test");
        jdbcTemplate.update("INSERT INTO ext_user_account_state(user_id,account_status,session_version,approved_at) VALUES(?,'APPROVED',1,NOW())",testAdminId);
        MvcResult adminLogin=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("username",testAdmin,"password",testAdminPassword))))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession adminSession=(MockHttpSession)adminLogin.getRequest().getSession(false);
        mockMvc.perform(post("/api/admin/accounts/"+userId+"/approve").session(adminSession).contentType(MediaType.APPLICATION_JSON).content("{\"roleCode\":\"REIMBURSEMENT\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(encryptedLogin("secure_user","Strong#123")))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(encryptedLogin("SECURE@EXAMPLE.TEST","Strong#123")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.username",is("secure_user")));

        MvcResult request=mockMvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"secure_user\",\"email\":\"secure@example.test\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.retryAfterSeconds",is(60))).andReturn();
        JsonNode json=objectMapper.readTree(request.getResponse().getContentAsString());
        String code=json.path("data").path("developmentResetCode").asText();assertTrue(code.matches("\\d{6}"));
        int tokenCount=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_password_reset_token WHERE user_id=?",Integer.class,userId);
        mockMvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"secure_user\",\"email\":\"secure@example.test\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.retryAfterSeconds",is(60)));
        int duplicateTokenCount=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_password_reset_token WHERE user_id=?",Integer.class,userId);
        assertTrue(tokenCount==1&&duplicateTokenCount==1);
        mockMvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"secure_user\",\"email\":\"secure@example.test\",\"code\":\""+code+"\",\"password\":\"Changed#456\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"secure_user\",\"password\":\"Changed#456\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/accounts/"+userId).session(adminSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data",is("账号已删除")));
        assertTrue(jdbcTemplate.queryForObject("SELECT account_status='DELETED' AND deleted_by=? AND deleted_at IS NOT NULL FROM ext_user_account_state WHERE user_id=?",Boolean.class,testAdminId,userId));
        assertFalse(jdbcTemplate.queryForObject("SELECT enabled FROM sys_user WHERE user_id=?",Boolean.class,userId));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"secure_user\",\"password\":\"Changed#456\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/accounts").session(adminSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.username == 'secure_user')]").isEmpty());
        mockMvc.perform(get("/api/admin/accounts").param("status","DELETED").session(adminSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.username == 'secure_user')].accountStatus").value("DELETED"));

        mockMvc.perform(post("/api/admin/accounts/"+userId+"/restore").session(adminSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data",is("账号已恢复")));
        assertTrue(jdbcTemplate.queryForObject("SELECT account_status='APPROVED' AND deleted_at IS NULL FROM ext_user_account_state WHERE user_id=?",Boolean.class,userId));
        assertTrue(jdbcTemplate.queryForObject("SELECT enabled FROM sys_user WHERE user_id=?",Boolean.class,userId));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"secure_user\",\"password\":\"Changed#456\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/accounts/"+testAdminId).session(adminSession))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message",is("不能删除当前登录账号")));
    }

    private String encryptedLogin(String username,String password) throws Exception {
        MvcResult response=mockMvc.perform(get("/api/auth/encryption-challenge")).andExpect(status().isOk()).andReturn();
        JsonNode data=objectMapper.readTree(response.getResponse().getContentAsString()).path("data");
        String challengeId=data.path("challengeId").asText();
        PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(data.path("publicKey").asText())));
        Cipher cipher=Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE,publicKey,new OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT));
        String encrypted=Base64.getEncoder().encodeToString(cipher.doFinal((challengeId+'\n'+password).getBytes(StandardCharsets.UTF_8)));
        return objectMapper.writeValueAsString(Map.of("username",username,"challengeId",challengeId,"encryptedPassword",encrypted));
    }
}

