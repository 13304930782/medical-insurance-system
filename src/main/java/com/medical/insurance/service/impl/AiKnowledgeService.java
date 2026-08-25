package com.medical.insurance.service.impl;

import com.medical.insurance.dao.AiMapper;
import com.medical.insurance.exception.AiBusinessException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiKnowledgeService {
    private static final int MAX_BYTES=2*1024*1024;
    private static final Pattern TITLE=Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private final JdbcTemplate jdbc;
    private final AiMapper mapper;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public AiKnowledgeService(JdbcTemplate jdbc,AiMapper mapper,AuthService authService,SystemMapper systemMapper){this.jdbc=jdbc;this.mapper=mapper;this.authService=authService;this.systemMapper=systemMapper;}

    public List<Map<String,Object>> documents(){return mapper.documents();}

    public Map<String,Object> helpDocument(Long requestedId){Long id=requestedId==null?mapper.defaultHelpDocumentId():requestedId;if(id==null)throw new AiBusinessException("系统帮助资料尚未初始化，请重新启动应用");Map<String,Object> document=mapper.helpDocument(id);if(document==null)throw new AiBusinessException("帮助资料不存在或已经失效");Map<String,Object> result=new LinkedHashMap<>(document);result.put("chunks",mapper.helpChunks(id));return result;}

    @Transactional
    public Map<String,Object> fetch(String rawUrl,HttpServletRequest servletRequest){
        return fetchInternal(rawUrl,authService.currentUserId(servletRequest),servletRequest.getRemoteAddr(),"管理员导入");
    }

    @Transactional
    public Map<String,Object> fetchAutomatically(String rawUrl){
        return fetchInternal(rawUrl,null,"127.0.0.1","国家医保局自动同步");
    }

    @Transactional
    public void upsertSystemDocument(String title,String content){
        String sourceUrl="system://medical-insurance/complete-guide";String normalized=content.replaceAll("\\r\\n?","\n").trim();String hash=sha256(normalized);Long id=mapper.documentByUrl(sourceUrl);
         if(id==null){id=insertSystemDocument(sourceUrl,title,hash);}else{String oldHash=jdbc.queryForObject("SELECT content_hash FROM ext_ai_knowledge_document WHERE document_id=?",String.class,id);if(hash.equals(oldHash))return;jdbc.update("DELETE FROM ext_ai_knowledge_chunk WHERE document_id=?",id);jdbc.update("UPDATE ext_ai_knowledge_document SET title=?,publisher='医疗保险报销系统项目',content_hash=?,fetched_at=NOW(),document_status='ACTIVE' WHERE document_id=?",title,hash,id);}
        List<String> values=systemChunks(normalized);for(int index=0;index<values.size();index++)jdbc.update("INSERT INTO ext_ai_knowledge_chunk(document_id,chunk_index,chunk_content) VALUES (?,?,?)",id,index,values.get(index));
    }

    private Map<String,Object> fetchInternal(String rawUrl,Long userId,String ipAddress,String operation){
        URI uri=officialUri(rawUrl);
        Long existingByUrl=mapper.documentByUrl(uri.toString());
        if(existingByUrl!=null)return Map.of("documentId",existingByUrl,"title",uri.toString(),"duplicate",true);
        HttpResponse<byte[]> response;
        try{
            HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(25)).header("User-Agent","MedicalInsuranceLearningSystem/1.0").GET().build();
            response=http().send(request,HttpResponse.BodyHandlers.ofByteArray());
         }catch(Exception e){throw new AiBusinessException("无法获取官方资料："+rootMessage(e));}
        if(response.statusCode()<200||response.statusCode()>=300)throw new AiBusinessException("官方资料请求失败，HTTP "+response.statusCode());
        if(response.body().length>MAX_BYTES)throw new AiBusinessException("资料超过2MB，请选择正文页面而不是大文件");
        String contentType=response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if(!contentType.contains("text/html")&&!contentType.contains("text/plain"))throw new AiBusinessException("当前只支持官方HTML或纯文本页面，不直接解析PDF/附件");
        String raw=new String(response.body(),StandardCharsets.UTF_8);
        String title=extractTitle(raw,uri);
        String text=contentType.contains("html")?htmlText(raw):raw.replaceAll("\\s+"," ").trim();
        if(text.length()<100)throw new AiBusinessException("页面正文过短，无法作为问答依据");
        String hash=sha256(text);
        Long existing=mapper.documentByHash(hash);
        if(existing!=null)return Map.of("documentId",existing,"title",title,"duplicate",true);
        long id=insertDocument(uri,title,hash);
        List<String> chunks=chunks(text);
        for(int index=0;index<chunks.size();index++)jdbc.update("INSERT INTO ext_ai_knowledge_chunk(document_id,chunk_index,chunk_content) VALUES (?,?,?)",id,index,chunks.get(index));
        systemMapper.recordOperation(userId,"AI政策知识库","IMPORT",String.valueOf(id),operation+"："+title,"SUCCESS",ipAddress);
        return Map.of("documentId",id,"title",title,"chunks",chunks.size(),"duplicate",false);
    }

    @Transactional
    public void delete(long id,HttpServletRequest request){if(mapper.deleteImportedDocument(id)==0)throw new AiBusinessException("内置系统使用指南不能删除，或该知识文档不存在");systemMapper.recordOperation(authService.currentUserId(request),"AI政策知识库","DELETE",String.valueOf(id),"删除知识文档","SUCCESS",request.getRemoteAddr());}

    private long insertDocument(URI uri,String title,String hash){GeneratedKeyHolder keys=new GeneratedKeyHolder();jdbc.update(connection->{PreparedStatement statement=connection.prepareStatement("INSERT INTO ext_ai_knowledge_document(source_type,source_url,title,publisher,fetched_at,content_hash,document_status) VALUES ('OFFICIAL_WEB',?,?,?,NOW(),?,'ACTIVE')",Statement.RETURN_GENERATED_KEYS);statement.setString(1,uri.toString());statement.setString(2,title);statement.setString(3,uri.getHost());statement.setString(4,hash);return statement;},keys);Number value=keys.getKey();if(value==null)throw new AiBusinessException("保存知识文档失败");return value.longValue();}
    private long insertSystemDocument(String sourceUrl,String title,String hash){GeneratedKeyHolder keys=new GeneratedKeyHolder();jdbc.update(connection->{PreparedStatement statement=connection.prepareStatement("INSERT INTO ext_ai_knowledge_document(source_type,source_url,title,publisher,fetched_at,content_hash,document_status) VALUES ('SYSTEM_DOCUMENT',?,?,'医疗保险报销系统项目',NOW(),?,'ACTIVE')",Statement.RETURN_GENERATED_KEYS);statement.setString(1,sourceUrl);statement.setString(2,title);statement.setString(3,hash);return statement;},keys);Number value=keys.getKey();if(value==null)throw new AiBusinessException("保存系统知识文档失败");return value.longValue();}
    private URI officialUri(String raw){try{URI uri=URI.create(raw==null?"":raw.trim());String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);if(!"https".equalsIgnoreCase(uri.getScheme()))throw new AiBusinessException("只允许HTTPS官方资料地址");if(!host.equals("gov.cn")&&!host.endsWith(".gov.cn"))throw new AiBusinessException("只允许导入中国政府或国家医保局的 .gov.cn 官方网站");if(uri.getUserInfo()!=null||uri.getPort()!=-1)throw new AiBusinessException("官方资料地址不能包含账号或自定义端口");return uri;}catch(IllegalArgumentException e){throw new AiBusinessException("官方资料URL格式不正确");}}
    private String extractTitle(String html,URI uri){Matcher matcher=TITLE.matcher(html);if(!matcher.find())return uri.getHost()+uri.getPath();return matcher.group(1).replaceAll("<[^>]+>","").replaceAll("\\s+"," ").trim();}
    private String htmlText(String html){StringBuilder result=new StringBuilder();try{new ParserDelegator().parse(new java.io.StringReader(html),new HTMLEditorKit.ParserCallback(){private boolean suppressed;public void handleText(char[] data,int pos){if(!suppressed)result.append(data).append(' ');}public void handleStartTag(HTML.Tag tag,MutableAttributeSet attrs,int pos){if(tag==HTML.Tag.SCRIPT||tag==HTML.Tag.STYLE)suppressed=true;}public void handleEndTag(HTML.Tag tag,int pos){if(tag==HTML.Tag.SCRIPT||tag==HTML.Tag.STYLE)suppressed=false;}},true);}catch(Exception e){throw new AiBusinessException("官方页面正文解析失败");}return result.toString().replaceAll("\\s+"," ").trim();}
    private List<String> systemChunks(String text){List<String> result=new ArrayList<>();for(String section:text.split("(?m)(?=^## )")){String value=section.trim();if(value.isEmpty())continue;if(value.length()<=2400)result.add(value);else result.addAll(chunks(value));}return result;}
    private List<String> chunks(String text){List<String> result=new ArrayList<>();int start=0;while(start<text.length()){int end=Math.min(text.length(),start+1200);if(end<text.length()){int boundary=Math.max(text.lastIndexOf('。',end),text.lastIndexOf('；',end));if(boundary>start+700)end=boundary+1;}result.add(text.substring(start,end));if(end==text.length())break;start=Math.max(start+1,end-150);}return result;}
    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String rootMessage(Throwable e){Throwable current=e;while(current.getCause()!=null)current=current.getCause();return current.getMessage()==null?current.getClass().getSimpleName():current.getMessage();}
    private HttpClient http(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();}
}
