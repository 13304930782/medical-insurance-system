package com.medical.insurance.config;

import com.medical.insurance.service.impl.AiKnowledgeService;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class AiSystemKnowledgeInitializer implements ApplicationRunner {
    private final AiKnowledgeService knowledgeService;

    AiSystemKnowledgeInitializer(AiKnowledgeService knowledgeService){this.knowledgeService=knowledgeService;}

    @Override
    public void run(ApplicationArguments arguments)throws Exception{
        ClassPathResource resource=new ClassPathResource("ai/system-knowledge.md");
        String content=new String(resource.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        knowledgeService.upsertSystemDocument("医疗保险报销系统完整使用、配置与故障处理指南",content);
    }
}
