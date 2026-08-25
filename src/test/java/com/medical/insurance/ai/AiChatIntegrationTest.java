package com.medical.insurance.ai;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties="app.ai.enabled=false")
@AutoConfigureMockMvc
@Transactional
@Rollback
class AiChatIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    private MockHttpSession session;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("INSERT INTO ext_ai_knowledge_document(source_type,source_url,title,publisher,content_hash,document_status) VALUES ('OFFICIAL_WEB','https://www.nhsa.gov.cn/test','国家医保测试政策','国家医疗保障局',REPEAT('a',64),'ACTIVE')");
        Long id=jdbc.queryForObject("SELECT document_id FROM ext_ai_knowledge_document WHERE content_hash=REPEAT('a',64)",Long.class);
        jdbc.update("INSERT INTO ext_ai_knowledge_chunk(document_id,chunk_index,chunk_content) VALUES (?,0,'医保起付标准应以当地有效政策和系统维护参数为准。')",id);
        String username="ai_test_admin_"+System.nanoTime(),password="Admin#1234";
        jdbc.update("INSERT INTO sys_user(username,password_hash,real_name,role_code,enabled) VALUES(?,?,?,?,1)",username,new BCryptPasswordEncoder().encode(password),"AI测试管理员","ADMIN");
        long userId=jdbc.queryForObject("SELECT user_id FROM sys_user WHERE username=?",Long.class,username);
        jdbc.update("INSERT INTO ext_user_security(user_id,email) VALUES(?,?)",userId,username+"@example.test");
        jdbc.update("INSERT INTO ext_user_account_state(user_id,account_status,session_version,approved_at) VALUES(?,'APPROVED',1,NOW())",userId);
        MvcResult result=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"password\":\""+password+"\"}"))
            .andExpect(status().isOk()).andReturn();
        session=(MockHttpSession)result.getRequest().getSession(false);
    }

    @Test
    void chatUsesOfficialEvidenceAndRefusesWhenModelOrEvidenceIsUnavailable() throws Exception {
        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"医保起付标准\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.evidenceSufficient",is(false)))
            .andExpect(jsonPath("$.data.answer",containsString("DeepSeek API尚未配置")))
            .andExpect(jsonPath("$.data.sources",hasSize(greaterThan(0))));

        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"完全无关的火星问题\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answer",containsString("现有系统指南和官方资料不足")))
            .andExpect(jsonPath("$.data.sources",hasSize(0)));
    }

    @Test
    void administratorCanReadAutomaticKnowledgeSyncStatus() throws Exception {
        mockMvc.perform(get("/api/ai/knowledge/sync-status").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled",is(false)))
            .andExpect(jsonPath("$.data.documentCount").isNumber())
            .andExpect(jsonPath("$.data.columns",hasSize(2)));
    }

    @Test
    void systemQuestionsRetrieveTheBuiltInCompleteGuide() throws Exception {
        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"系统里怎么导入Excel数据\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answer",containsString("DeepSeek API尚未配置")))
            .andExpect(jsonPath("$.data.sources",hasSize(1)))
            .andExpect(jsonPath("$.data.sources[0].type",is("SYSTEM_DOCUMENT")))
            .andExpect(jsonPath("$.data.citations",hasSize(greaterThan(5))))
            .andExpect(jsonPath("$.data.citations[0].chunkId").isNumber());

        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"服务设施为什么不能选择西药\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sources[?(@.type == 'SYSTEM_DOCUMENT')]",hasSize(greaterThan(0))));

        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"登录成功以后又跳回登录页怎么排查\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sources[?(@.type == 'SYSTEM_DOCUMENT')]",hasSize(greaterThan(0))));

        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"我报销的时候提示人员未进行特检审批怎么办\",\"page\":\"reimbursement\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answer",containsString("DeepSeek API尚未配置")))
            .andExpect(jsonPath("$.data.sources",hasSize(1)))
            .andExpect(jsonPath("$.data.sources[0].type",is("SYSTEM_DOCUMENT")));
    }

    @Test
    void onlyExplicitPolicyQuestionsAskForTheInsuredLocation() throws Exception {
        mockMvc.perform(post("/api/ai/chat").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"国家医保政策里的门诊报销怎么申请\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answer",containsString("请补充你的参保省市")))
            .andExpect(jsonPath("$.data.sources",hasSize(0)));
    }

    @Test
    void builtInSystemGuideCannotBeDeleted() throws Exception {
        long documentId=jdbc.queryForObject("SELECT document_id FROM ext_ai_knowledge_document WHERE source_type='SYSTEM_DOCUMENT' AND source_url='system://medical-insurance/complete-guide'",Long.class);
        mockMvc.perform(delete("/api/ai/knowledge/documents/"+documentId).session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message",containsString("内置系统使用指南不能删除")));
    }

    @Test
    void authenticatedUsersCanOpenHelpDocumentAndItsChunks() throws Exception {
        long documentId=jdbc.queryForObject("SELECT document_id FROM ext_ai_knowledge_document WHERE source_type='SYSTEM_DOCUMENT' AND source_url='system://medical-insurance/complete-guide'",Long.class);
        mockMvc.perform(get("/api/ai/help").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sourceType",is("SYSTEM_DOCUMENT")))
            .andExpect(jsonPath("$.data.chunks",hasSize(greaterThan(5))))
            .andExpect(jsonPath("$.data.chunks[0].chunkId").isNumber());
        mockMvc.perform(get("/api/ai/help/documents/"+documentId).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.documentId",is((int)documentId)));
    }
}
