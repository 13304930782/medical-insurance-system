package com.medical.insurance.service.impl;

import com.medical.insurance.dao.AiMapper;
import com.medical.insurance.exception.AiBusinessException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiChatService {
    private static final String PROMPT_VERSION="reasoned-system-help-and-policy-v4";
    private static final String REFUSAL="现有系统指南和官方资料不足以可靠回答这个问题。请换一种说法并说明所在页面、当前角色或具体报错；政策问题也可以补充参保地区。";
    private final AiMapper mapper;
    private final JdbcTemplate jdbc;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final NhsaPolicySyncService syncService;
    @Value("${app.ai.enabled:false}") boolean enabled;
    @Value("${app.ai.api-key:}") String apiKey;
    @Value("${app.ai.base-url:https://api.deepseek.com}") String baseUrl;
    @Value("${app.ai.model:deepseek-v4-flash}") String model;
    @Value("${app.ai.thinking-enabled:true}") boolean thinkingEnabled;

    public AiChatService(AiMapper mapper,JdbcTemplate jdbc,AuthService authService,ObjectMapper objectMapper,NhsaPolicySyncService syncService){this.mapper=mapper;this.jdbc=jdbc;this.authService=authService;this.objectMapper=objectMapper;this.syncService=syncService;}

    public Map<String,Object> ask(String rawQuestion,String currentPage,HttpServletRequest request){
        long started=System.currentTimeMillis();String requestId=UUID.randomUUID().toString();String question=rawQuestion==null?"":rawQuestion.trim();
        if(question.length()<2||question.length()>1000)throw new AiBusinessException("问题长度应为2至1000字");
        boolean systemQuestion=isSystemQuestion(question);
        String clarification=!systemQuestion&&isExplicitPolicyQuestion(question)?locationClarification(question):null;
        if(clarification!=null)return finish(requestId,question,clarification,false,List.of(),List.of(),"NEEDS_LOCATION",started,request);
        List<Map<String,Object>> evidence=systemQuestion?mapper.systemEvidence():relevantEvidence(question,mapper.search(question,keyword(question)));
         if(evidence.isEmpty()&&!systemQuestion){syncService.synchronizeForQuestion(question);evidence=relevantEvidence(question,mapper.search(question,keyword(question)));}
        List<Map<String,Object>> sources=sources(evidence);
        List<Map<String,Object>> citations=citations(evidence);
        if(evidence.isEmpty())return finish(requestId,question,REFUSAL,false,sources,citations,"NO_EVIDENCE",started,request);
        if(!enabled||apiKey==null||apiKey.isBlank())return finish(requestId,question,"已找到相关系统指南或官方资料，但DeepSeek API尚未配置。请管理员设置 DEEPSEEK_API_KEY 并启用 AI_ENABLED。",false,sources,citations,"MODEL_DISABLED",started,request);
        String answer=callDeepSeek(question,evidence,authService.currentRoleCode(request),normalizePage(currentPage));
        boolean cited=answer.matches("(?s).*\\[资料\\d+].*");
         if(!cited){answer=REFUSAL;sources=List.of();citations=List.of();}
        return finish(requestId,question,answer,cited,sources,citations,cited?"SUCCESS":"UNVERIFIED",started,request);
    }

    private String callDeepSeek(String question,List<Map<String,Object>> evidence,String currentRole,String currentPage){
        StringBuilder context=new StringBuilder();for(int index=0;index<evidence.size();index++){Map<String,Object> row=evidence.get(index);String type="SYSTEM_DOCUMENT".equals(row.get("sourceType"))?"系统资料":"官方政策";context.append("[资料").append(index+1).append("] 类型：").append(type).append("\n标题：").append(row.get("title")).append("\n来源：").append(row.get("sourceUrl")).append("\n正文：").append(row.get("chunkContent")).append("\n\n");}
        String system="你是本项目内置的医疗保险报销系统使用助手，同时可以依据官方资料解释医保政策。先在内部认真判断用户是在询问本系统操作/报错，还是明确询问国家或地方政策；不要向用户展示内部推理过程。只能依据用户消息中的【可用资料】回答，禁止凭训练记忆补充本系统不存在的按钮、字段或功能。回答系统使用问题时：1. 直接分析用户描述的提示或失败原因，不要机械要求参保地；2. 给出准确菜单路径和按顺序排列的操作步骤；3. 结合当前登录角色和页面判断菜单是否可见，没有权限时说明需要哪个角色；4. 对报错列出最可能原因和可执行检查；5. 用户问字段时说明填写规则、联动关系和保存影响；6. 问法不完整时先回答能够确定的部分，再只追问一个必要信息。只有明确询问国家、地方、异地就医或最新医保政策时才使用官方政策证据和地区条件。每个事实段必须用[资料N]标注来源。资料不足或冲突时明确说明，不得编造。不得读取或索要密码、数据库口令、API Key，不得声称已经替用户执行新增、修改、删除、审批、结算或取消操作。";
        String user="当前登录角色："+currentRole+"\n当前所在页面："+(currentPage.isBlank()?"未提供":currentPage)+"\n用户问题："+question+"\n\n【可用资料】\n"+context;
        try{
            HttpResponse<String> response=sendDeepSeek(deepSeekPayload(system,user,thinkingEnabled));
            if(thinkingEnabled&&(response.statusCode()==400||response.statusCode()==422))response=sendDeepSeek(deepSeekPayload(system,user,false));
            if(response.statusCode()<200||response.statusCode()>=300)throw new AiBusinessException("DeepSeek服务请求失败，HTTP "+response.statusCode());
            JsonNode root=objectMapper.readTree(response.body());String answer=root.path("choices").path(0).path("message").path("content").asText();if(answer==null||answer.isBlank())throw new AiBusinessException("DeepSeek未返回回答");return answer.trim();
         }catch(AiBusinessException e){throw e;}catch(Exception e){throw new AiBusinessException("DeepSeek服务暂时不可用："+rootMessage(e));}
    }

    private Map<String,Object> deepSeekPayload(String system,String user,boolean useThinking){return Map.of("model",model,"thinking",Map.of("type",useThinking?"enabled":"disabled"),"temperature",0,"max_tokens",1800,"stream",false,"messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",user)));}
    private HttpResponse<String> sendDeepSeek(Map<String,Object> payload)throws Exception{HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$","")+"/chat/completions")).timeout(Duration.ofSeconds(45)).header("Authorization","Bearer "+apiKey).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload),StandardCharsets.UTF_8)).build();return http().send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}

    private Map<String,Object> finish(String requestId,String question,String answer,boolean sufficient,List<Map<String,Object>> sources,List<Map<String,Object>> citations,String result,long started,HttpServletRequest request){
         try{jdbc.update("INSERT INTO ext_ai_query_log(request_id,user_id,question_masked,answer_text,evidence_sufficient,source_ids,model_name,prompt_version,duration_ms,query_result) VALUES (?,?,?,?,?,?,?,?,?,?)",requestId,authService.currentUserId(request),mask(question),answer,sufficient,objectMapper.writeValueAsString(sources.stream().map(source->source.get("documentId")).distinct().toList()),model,PROMPT_VERSION,System.currentTimeMillis()-started,result);}catch(Exception ignored){}
        Map<String,Object> response=new LinkedHashMap<>();response.put("requestId",requestId);response.put("answer",answer);response.put("evidenceSufficient",sufficient);response.put("sources",sources);response.put("citations",citations);return response;
    }
    private List<Map<String,Object>> sources(List<Map<String,Object>> rows){List<Map<String,Object>> result=new ArrayList<>();for(Map<String,Object> row:rows){Map<String,Object> source=new LinkedHashMap<>();source.put("documentId",row.get("documentId"));source.put("title",row.get("title"));source.put("type",row.get("sourceType"));source.put("url",row.get("sourceUrl"));if(result.stream().noneMatch(existing->String.valueOf(existing.get("documentId")).equals(String.valueOf(source.get("documentId")))))result.add(source);}return result;}
    private List<Map<String,Object>> citations(List<Map<String,Object>> rows){List<Map<String,Object>> result=new ArrayList<>();for(int index=0;index<rows.size();index++){Map<String,Object> row=rows.get(index),citation=new LinkedHashMap<>();citation.put("number",index+1);citation.put("documentId",row.get("documentId"));citation.put("chunkId",row.get("chunkId"));citation.put("title",row.get("title"));citation.put("type",row.get("sourceType"));citation.put("url",row.get("sourceUrl"));result.add(citation);}return result;}
    private List<Map<String,Object>> relevantEvidence(String question,List<Map<String,Object>> rows){Set<String> grams=questionGrams(question);if(grams.isEmpty())return rows;boolean systemQuestion=isSystemQuestion(question);int policyThreshold=grams.size()<3?1:Math.min(4,Math.max(2,(grams.size()+1)/2));return rows.stream().filter(row->{boolean systemSource="SYSTEM_DOCUMENT".equals(row.get("sourceType"));if(systemSource&&!systemQuestion)return false;String text=String.valueOf(row.get("title"))+" "+String.valueOf(row.get("chunkContent"));long score=grams.stream().filter(text::contains).count();int threshold=systemSource?Math.min(2,grams.size()):policyThreshold;return score>=threshold;}).toList();}
    private Set<String> questionGrams(String question){String clean=question.replaceAll("[^\\p{IsHan}A-Za-z]","");Set<String> result=new LinkedHashSet<>();for(int index=0;index+1<clean.length();index++){String gram=clean.substring(index,index+2);if(!Set.of("如何","怎么","什么","是否","可以","需要","请问","我在","本地","当地").contains(gram))result.add(gram);}return result;}
    private boolean isSystemQuestion(String question){
        if(question.matches("(?is).*(本系统|系统里|这个系统|当前系统|页面|菜单|按钮|Dashboard).*"))return true;
        if(isExplicitPolicyQuestion(question))return false;
        return question.matches("(?is).*(登录|注册|忘记密码|验证码|账号|邮箱|用户|人员|管理员|角色|权限|药品|诊疗项目|服务设施|病种|定点机构|单位信息|个人信息|待遇参数|封顶线|起付标准|分段比例|审批|特检|特治|就诊|门诊号|住院号|处方|报销|预结算|正式结算|结算清单|取消|综合查询|打印|导入|导出|批量|操作日志|测试数据|Maven|IDEA|Navicat|MySQL|DeepSeek|AI问答|SMTP|邮件|HTTPS|8080|端口|提示|报错|错误|失败|异常|打不开|找不到|不能选择|怎么办|如何操作|怎么操作).*" );
    }
    private boolean isExplicitPolicyQuestion(String question){return question.matches("(?is).*(国家医保局|国家规定|全国政策|医保政策|政策规定|法律法规|参保地政策|地方政策|异地就医备案|最新通知|最新政策).*" )||question.matches(".*(北京|天津|河北|山西|内蒙古|辽宁|吉林|黑龙江|上海|江苏|浙江|安徽|福建|江西|山东|河南|湖北|湖南|广东|广西|海南|重庆|四川|贵州|云南|西藏|陕西|甘肃|青海|宁夏|新疆|兵团).*(医保|报销).*" );}
    private String locationClarification(String question){if(isSystemQuestion(question))return null;boolean processQuestion=question.matches(".*(门诊|报销).*(如何|怎么|流程|申请).*|.*(如何|怎么|流程|申请).*(门诊|报销).*");if(!processQuestion)return null;String locations="北京|天津|河北|山西|内蒙古|辽宁|吉林|黑龙江|上海|江苏|浙江|安徽|福建|江西|山东|河南|湖北|湖南|广东|广西|海南|重庆|四川|贵州|云南|西藏|陕西|甘肃|青海|宁夏|新疆|兵团";if(question.matches(".*("+locations+").*"))return null;return "门诊报销流程通常由参保地政策决定。请补充你的参保省市，并说明是职工医保还是城乡居民医保；我再从国家医保局官方资料中检索相应依据。";}
    private String keyword(String question){String value=question.replaceAll("[\\p{P}\\p{S}\\s]","");return value.length()>12?value.substring(0,12):value;}
    private String normalizePage(String value){String page=value==null?"":value.trim();return page.matches("[A-Za-z]{2,40}")?page:"";}
    private String mask(String question){return question.replaceAll("(?<!\\d)\\d{17}[0-9Xx](?!\\d)","******************").replaceAll("(?<!\\d)1\\d{10}(?!\\d)","***********");}
    private String rootMessage(Throwable error){Throwable current=error;while(current.getCause()!=null)current=current.getCause();return current.getMessage()==null?current.getClass().getSimpleName():current.getMessage();}
    private HttpClient http(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();}
}
