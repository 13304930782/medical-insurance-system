package com.medical.insurance.person;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class PersonApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void personCrudUsesEveryOriginalPeopleColumnWithoutRenamingIt() throws Exception {
        MockHttpSession session = TestAdminSession.create(mockMvc,jdbcTemplate);

        String createJson = "{"
            + "\"peopleId\":\"TEST-PERSON-001\","
            + "\"idType\":\"居民身份证\","
            + "\"id\":\"TEST-ID-001\","
            + "\"name\":\"测试人员\","
            + "\"sex\":\"男\","
            + "\"nationality\":\"汉族\","
            + "\"brithday\":\"1990-01-02\","
            + "\"residenceType\":\"城镇户口\","
            + "\"residenceAdress\":\"测试户口所在地\","
            + "\"medicalPersonnel\":\"在职\","
            + "\"authorizedStrength\":\"在编\","
            + "\"note\":\"个人信息回归测试\"}";

        mockMvc.perform(post("/api/people").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(createJson))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/people/TEST-PERSON-001").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name", is("测试人员")))
            .andExpect(jsonPath("$.data.brithday", is("1990-01-02")))
            .andExpect(jsonPath("$.data.residenceAdress", is("测试户口所在地")))
            .andExpect(jsonPath("$.data.socialSecurityId", matchesPattern("[0-9a-f]{32}")));

        assertEquals("TEST-ID-001", jdbcTemplate.queryForObject(
            "SELECT `ID` FROM people WHERE people_id=?", String.class, "TEST-PERSON-001"));
        assertEquals(LocalDate.of(1990, 1, 2), jdbcTemplate.queryForObject(
            "SELECT brithday FROM people WHERE people_id=?", LocalDate.class, "TEST-PERSON-001"));
        assertEquals("测试户口所在地", jdbcTemplate.queryForObject(
            "SELECT residence_adress FROM people WHERE people_id=?", String.class, "TEST-PERSON-001"));

        mockMvc.perform(put("/api/people/TEST-PERSON-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson.replace("测试人员", "测试人员（更新）")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/people").session(session).param("keyword", "更新"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].peopleId", is("TEST-PERSON-001")));

        mockMvc.perform(delete("/api/people/TEST-PERSON-001").session(session))
            .andExpect(status().isOk());
    }
}

