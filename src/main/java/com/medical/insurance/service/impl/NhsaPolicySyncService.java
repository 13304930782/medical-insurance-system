package com.medical.insurance.service.impl;

import com.medical.insurance.dao.AiMapper;
import com.medical.insurance.exception.AiBusinessException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NhsaPolicySyncService {
    private static final Pattern ARTICLE_LINK=Pattern.compile("(?i)href\\s*=\\s*[\\\"'](?<url>[^\\\"']*/art/[^\\\"']+\\.html(?:\\?[^\\\"']*)?)[\\\"']");
    private static final int MAX_LIST_BYTES=3*1024*1024;
    private final AiKnowledgeService knowledgeService;
    private final AiMapper mapper;
    private final AtomicBoolean running=new AtomicBoolean(false);
    @Value("${app.ai.sync.enabled:false}") boolean enabled;
    @Value("${app.ai.sync.columns:https://www.nhsa.gov.cn/col/col104/index.html,https://www.nhsa.gov.cn/col/col105/index.html}") String columns;
    @Value("${app.ai.sync.max-articles-per-column:12}") int maxArticlesPerColumn;
    @Value("${app.ai.sync.max-query-articles:5}") int maxQueryArticles;
    private volatile LocalDateTime lastStartedAt;
    private volatile LocalDateTime lastCompletedAt;
    private volatile String lastResult="尚未执行";
    private volatile int lastDiscovered;
    private volatile int lastImported;
    private volatile int lastSkipped;
    private volatile int lastFailed;

    public NhsaPolicySyncService(AiKnowledgeService knowledgeService,AiMapper mapper){this.knowledgeService=knowledgeService;this.mapper=mapper;}

    @Scheduled(initialDelayString="${app.ai.sync.initial-delay-ms:15000}",fixedDelayString="${app.ai.sync.fixed-delay-ms:86400000}")
    public void scheduledSync(){if(enabled)synchronize();}

    public Map<String,Object> synchronize(){
        if(!running.compareAndSet(false,true))return status();
        lastStartedAt=LocalDateTime.now();lastResult="同步中";lastDiscovered=0;lastImported=0;lastSkipped=0;lastFailed=0;
        try{
             for(URI column:columnUrls()){
                 for(URI article:discover(column)){
                    lastDiscovered++;
                    try{
                        Map<String,Object> result=knowledgeService.fetchAutomatically(article.toString());
                        if(Boolean.TRUE.equals(result.get("duplicate")))lastSkipped++;else lastImported++;
                     }catch(Exception exception){lastFailed++;}
                }
            }
            lastResult=lastFailed==0?"同步完成":"同步完成，部分资料失败";
         }catch(Exception exception){lastResult="同步失败："+rootMessage(exception);lastFailed++;}
        finally{lastCompletedAt=LocalDateTime.now();running.set(false);}
        return status();
    }

    public void synchronizeForQuestion(String question){
        if(!enabled||!running.compareAndSet(false,true))return;
        lastStartedAt=LocalDateTime.now();lastResult="正在按问题检索国家医保局资料";lastDiscovered=0;lastImported=0;lastSkipped=0;lastFailed=0;
        try{
             for(URI article:discoverByQuestion(question)){
                lastDiscovered++;
                try{
                    Map<String,Object> result=knowledgeService.fetchAutomatically(article.toString());
                    if(Boolean.TRUE.equals(result.get("duplicate")))lastSkipped++;else lastImported++;
                 }catch(Exception exception){lastFailed++;}
            }
            lastResult=lastFailed==0?"按问题同步完成":"按问题同步完成，部分资料失败";
         }catch(Exception exception){lastResult="按问题同步失败："+rootMessage(exception);lastFailed++;}
        finally{lastCompletedAt=LocalDateTime.now();running.set(false);}
    }

    public Map<String,Object> status(){
        Map<String,Object> result=new LinkedHashMap<>();Map<String,Object> stats=mapper.knowledgeStats();
        result.put("enabled",enabled);result.put("running",running.get());result.put("result",lastResult);
        result.put("lastStartedAt",lastStartedAt);result.put("lastCompletedAt",lastCompletedAt);
        result.put("lastDiscovered",lastDiscovered);result.put("lastImported",lastImported);result.put("lastSkipped",lastSkipped);result.put("lastFailed",lastFailed);
        result.put("documentCount",stats==null?0:stats.getOrDefault("documentCount",0));result.put("lastFetchedAt",stats==null?null:stats.get("lastFetchedAt"));result.put("columns",columnUrls().stream().map(URI::toString).toList());
        return result;
    }

    private List<URI> columnUrls(){List<URI> result=new ArrayList<>();for(String value:columns.split(",")){String trimmed=value.trim();if(!trimmed.isEmpty())result.add(nhsaUri(trimmed));}return result;}

    private List<URI> discover(URI column){
        try{
            HttpRequest request=HttpRequest.newBuilder(column).timeout(Duration.ofSeconds(25)).header("User-Agent","MedicalInsuranceLearningSystem/1.0 (+localhost policy sync)").GET().build();
            HttpResponse<byte[]> response=http().send(request,HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()<200||response.statusCode()>=300)throw new AiBusinessException("国家医保局栏目请求失败，HTTP "+response.statusCode());
            if(response.body().length>MAX_LIST_BYTES)throw new AiBusinessException("国家医保局栏目页面过大");
            String html=new String(response.body(),StandardCharsets.UTF_8);Matcher matcher=ARTICLE_LINK.matcher(html);Set<URI> links=new LinkedHashSet<>();
             while(matcher.find()&&links.size()<Math.max(1,Math.min(maxArticlesPerColumn,50))){URI article=nhsaUri(column.resolve(matcher.group("url")).toString());links.add(article);}
            return List.copyOf(links);
         }catch(AiBusinessException exception){throw exception;}catch(Exception exception){throw new AiBusinessException("无法读取国家医保局政策列表："+rootMessage(exception));}
    }

    private List<URI> discoverByQuestion(String question){
        try{
            String keywords=safeSearchKeywords(question);
            URI search=nhsaUri("https://www.nhsa.gov.cn/jrobot/search.do?webid=1&q="+URLEncoder.encode(keywords,StandardCharsets.UTF_8));
            HttpRequest request=HttpRequest.newBuilder(search).timeout(Duration.ofSeconds(25)).header("User-Agent","MedicalInsuranceLearningSystem/1.0 (+localhost policy search)").GET().build();
            HttpResponse<byte[]> response=http().send(request,HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()<200||response.statusCode()>=300)throw new AiBusinessException("国家医保局站内搜索失败，HTTP "+response.statusCode());
            if(response.body().length>MAX_LIST_BYTES)throw new AiBusinessException("国家医保局搜索页面过大");
            String html=new String(response.body(),StandardCharsets.UTF_8);Matcher matcher=ARTICLE_LINK.matcher(html);Set<URI> links=new LinkedHashSet<>();
             while(matcher.find()&&links.size()<Math.max(1,Math.min(maxQueryArticles,10))){links.add(articleUri(search,matcher.group("url")));}
            return List.copyOf(links);
         }catch(AiBusinessException exception){throw exception;}catch(Exception exception){throw new AiBusinessException("无法按问题检索国家医保局资料："+rootMessage(exception));}
    }

    private String safeSearchKeywords(String question){
        String[] vocabulary={"门诊慢特病","异地就医","职工医保","居民医保","大病保险","医疗救助","门诊","住院","报销","起付线","封顶线","医保","生育保险","慢特病","药品目录","个人账户","家庭共济","转诊","备案","直接结算","参保","缴费","定点医疗机构"};
        List<String> words=new ArrayList<>();String value=question==null?"":question;
        for(String word:vocabulary)if(value.contains(word)&&!words.contains(word))words.add(word);
        String locations="北京 天津 河北 山西 内蒙古 辽宁 吉林 黑龙江 上海 江苏 浙江 安徽 福建 江西 山东 河南 湖北 湖南 广东 广西 海南 重庆 四川 贵州 云南 西藏 陕西 甘肃 青海 宁夏 新疆 兵团";
        for(String location:locations.split(" "))if(value.contains(location))words.add(location);
        return words.isEmpty()?"医疗保障政策":String.join(" ",words);
    }

    private URI articleUri(URI search,String raw){URI value=search.resolve(raw.replace("http://www.nhsa.gov.cn/","https://www.nhsa.gov.cn/").replace("http://nhsa.gov.cn/","https://nhsa.gov.cn/"));return nhsaUri(value.toString());}

    private URI nhsaUri(String raw){try{URI uri=URI.create(raw);String host=uri.getHost()==null?"":uri.getHost().toLowerCase();if(!"https".equalsIgnoreCase(uri.getScheme())||!(host.equals("nhsa.gov.cn")||host.equals("www.nhsa.gov.cn"))||uri.getPort()!=-1||uri.getUserInfo()!=null)throw new AiBusinessException("自动同步只允许国家医疗保障局官网");return uri;}catch(IllegalArgumentException exception){throw new AiBusinessException("国家医保局同步地址格式错误");}}
    private String rootMessage(Throwable error){Throwable current=error;while(current.getCause()!=null)current=current.getCause();return current.getMessage()==null?current.getClass().getSimpleName():current.getMessage();}
    private HttpClient http(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();}
}
