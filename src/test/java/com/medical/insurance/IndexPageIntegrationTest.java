package com.medical.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IndexPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void indexTemplateRendersCompletely() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"loginForm\"")))
                .andExpect(content().string(containsString("id=\"registerForm\"")))
                .andExpect(content().string(containsString("id=\"forgotRequestForm\"")))
                .andExpect(content().string(containsString("id=\"forgotRequestButton\"")))
                .andExpect(content().string(containsString("id=\"resetPasswordForm\"")))
                .andExpect(content().string(containsString("id=\"changePasswordForm\"")))
                .andExpect(content().string(containsString("href=\"/css/app.css?v=")))
                .andExpect(content().string(containsString("src=\"/js/app.js?v=")))
                .andExpect(content().string(containsString("data-submit-form=\"loginForm\"")))
                .andExpect(content().string(containsString("用户名或邮箱")))
                .andExpect(content().string(not(containsString("初始测试账号"))))
                .andExpect(content().string(not(containsString("admin / 123456"))))
                .andExpect(content().string(containsString("id=\"treatmentMenu\"")))
                .andExpect(content().string(containsString("id=\"segmentForm\"")))
                .andExpect(content().string(containsString("id=\"institutionApprovalMenu\"")))
                .andExpect(content().string(containsString("id=\"specialApprovalForm\"")))
                .andExpect(content().string(containsString("id=\"reimbursementMenu\"")))
                .andExpect(content().string(containsString("id=\"dashboardMenu\"")))
                .andExpect(content().string(containsString("id=\"dashboardPage\"")))
                .andExpect(content().string(containsString("id=\"dashboardSubjectList\"")))
                .andExpect(content().string(containsString("id=\"companyTypePie\"")))
                .andExpect(content().string(containsString("id=\"aiChatLauncher\"")))
                .andExpect(content().string(containsString("按住可拖动")))
                .andExpect(content().string(containsString("id=\"aiChatWidget\"")))
                .andExpect(content().string(containsString("id=\"aiChatMessages\"")))
                .andExpect(content().string(containsString("id=\"aiChatInput\"")))
                .andExpect(content().string(containsString("id=\"aiSyncButton\"")))
                .andExpect(content().string(containsString("id=\"helpMenu\"")))
                .andExpect(content().string(containsString("id=\"helpPage\"")))
                .andExpect(content().string(containsString("id=\"helpBackButton\"")))
                .andExpect(content().string(containsString("id=\"helpDocumentBody\"")))
                .andExpect(content().string(containsString("src=\"/js/core/state.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/core/common.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/core/navigation.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/modules/auth.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/modules/catalog.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/modules/master-data.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/modules/treatment-approval.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/modules/reimbursement.js?v=")))
                .andExpect(content().string(containsString("src=\"/js/modules/dashboard.js?v=")))
                .andExpect(content().string(containsString("id=\"prescriptionForm\"")))
                .andExpect(content().string(containsString("data-clear-name=\"projectCoding\"")))
                .andExpect(content().string(containsString("list=\"prescriptionExpenseTypeOptions\"")))
                .andExpect(content().string(containsString("data-clear-name=\"projectCode\"")))
                .andExpect(content().string(containsString("id=\"settlementQueryMenu\"")))
                .andExpect(content().string(containsString("id=\"bulkMenu\"")))
                .andExpect(content().string(containsString("id=\"bulkPage\"")))
                .andExpect(content().string(containsString("id=\"settlementModal\"")))
                .andExpect(content().string(containsString("</script>")))
                .andExpect(content().string(containsString("</html>")));

        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("function bootstrap")))
                .andExpect(content().string(containsString("/api/auth/me")));

        mockMvc.perform(get("/js/core/common.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("credentials: \"include\"")))
                .andExpect(content().string(containsString("ensureTablePager")))
                .andExpect(content().string(containsString("requestTablePage")))
                .andExpect(content().string(containsString("paginateClientTable")));

        mockMvc.perform(get("/js/modules/auth.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("encryptPasswordPayload")))
                .andExpect(content().string(containsString("startForgotRequestCooldown")))
                .andExpect(content().string(containsString("const sessionUser = await api(\"/api/auth/me\")")))
                .andExpect(content().string(containsString("$(\"loginForm\").addEventListener")));

        mockMvc.perform(get("/js/modules/treatment-approval.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/approvals/special/projects")));

        mockMvc.perform(get("/js/modules/reimbursement.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("renderPrescriptionCategoryOptions")))
                .andExpect(content().string(containsString("renderCatalogProjectOptions")))
                .andExpect(content().string(containsString("previewSettlement")));

        mockMvc.perform(get("/js/modules/ai-assistant.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("appendAiMessage")))
                .andExpect(content().string(containsString("initAiLauncherDrag")))
                .andExpect(content().string(containsString("renderAiAnswer")))
                .andExpect(content().string(containsString("/api/ai/help/documents/")))
                .andExpect(content().string(containsString("/api/ai/knowledge/sync")));

        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".company-pie")))
                .andExpect(content().string(containsString(".ai-chat-launcher")))
                .andExpect(content().string(containsString(".ai-chat-widget")))
                .andExpect(content().string(containsString(".page-jump")))
                .andExpect(content().string(containsString(".client-page-hidden")));

        mockMvc.perform(get("/js/modules/dashboard.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("selectDashboardCompany")))
                .andExpect(content().string(containsString("previewDashboardSettlement")))
                .andExpect(content().string(containsString("renderCompanyTypePie")))
                .andExpect(content().string(containsString("paginateClientTable(\"dashboardVisit\"")));
    }
}

