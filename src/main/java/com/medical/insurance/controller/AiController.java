package com.medical.insurance.controller;

import com.medical.insurance.exception.AiBusinessException;
import com.medical.insurance.service.impl.AiChatService;
import com.medical.insurance.service.impl.AiKnowledgeService;
import com.medical.insurance.service.impl.NhsaPolicySyncService;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiKnowledgeService knowledgeService;
    private final AiChatService chatService;
    private final NhsaPolicySyncService syncService;
    AiController(AiKnowledgeService knowledgeService,AiChatService chatService,NhsaPolicySyncService syncService){this.knowledgeService=knowledgeService;this.chatService=chatService;this.syncService=syncService;}

    @PostMapping("/chat") Map<String,Object> chat(@RequestBody Map<String,String> body,HttpServletRequest request){return success(chatService.ask(body.get("question"),body.get("page"),request));}
    @GetMapping("/help") Map<String,Object> defaultHelp(){return success(knowledgeService.helpDocument(null));}
    @GetMapping("/help/documents/{id}") Map<String,Object> helpDocument(@PathVariable long id){return success(knowledgeService.helpDocument(id));}
    @GetMapping("/knowledge/documents") Map<String,Object> documents(){return success(knowledgeService.documents());}
    @PostMapping("/knowledge/fetch") Map<String,Object> fetch(@RequestBody Map<String,String> body,HttpServletRequest request){return success(knowledgeService.fetch(body.get("url"),request));}
    @GetMapping("/knowledge/sync-status") Map<String,Object> syncStatus(){return success(syncService.status());}
    @PostMapping("/knowledge/sync") Map<String,Object> synchronize(){return success(syncService.synchronize());}
    @DeleteMapping("/knowledge/documents/{id}") Map<String,Object> delete(@PathVariable long id,HttpServletRequest request){knowledgeService.delete(id,request);return success("删除成功");}
    @ExceptionHandler(AiBusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> error(AiBusinessException e){Map<String,Object> result=new LinkedHashMap<>();result.put("success",false);result.put("message",e.getMessage());return result;}
    private Map<String,Object> success(Object data){Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("data",data);return result;}
}
