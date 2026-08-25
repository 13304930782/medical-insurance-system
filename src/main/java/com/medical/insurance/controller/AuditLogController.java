package com.medical.insurance.controller;

import com.medical.insurance.dao.AuditLogMapper;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogMapper mapper;
    AuditLogController(AuditLogMapper mapper){this.mapper=mapper;}

    @GetMapping
    Map<String,Object> logs(@RequestParam(required=false)String keyword,@RequestParam(required=false)String result,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){String q=normalize(keyword),r=normalizeResult(result);page=Math.max(page,1);size=Math.min(Math.max(size,1),200);long total=mapper.count(q,r);Map<String,Object> data=new LinkedHashMap<>();data.put("items",mapper.page(q,r,(page-1)*size,size));data.put("total",total);data.put("page",page);data.put("size",size);data.put("totalPages",total==0?0:(total+size-1)/size);return success(data);}

    @GetMapping("/export.xlsx")
    ResponseEntity<byte[]> export(@RequestParam(required=false)String keyword,@RequestParam(required=false)String result)throws Exception{List<Map<String,Object>> rows=mapper.export(normalize(keyword),normalizeResult(result));try(XSSFWorkbook workbook=new XSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()){Sheet sheet=workbook.createSheet("操作日志");String[] headers={"日志ID","操作账号","操作人","业务模块","做了什么","业务编号","操作详情","结果","IP地址","操作时间"};Row header=sheet.createRow(0);for(int i=0;i<headers.length;i++)header.createCell(i).setCellValue(headers[i]);String[] keys={"logId","username","realName","operationModule","operationLabel","businessNo","operationContent","operationResult","ipAddress","createdAt"};for(int n=0;n<rows.size();n++){Row row=sheet.createRow(n+1);for(int i=0;i<keys.length;i++){Object value=rows.get(n).get(keys[i]);if("operationResult".equals(keys[i])&&value!=null)value="SUCCESS".equals(String.valueOf(value))?"成功":"失败";row.createCell(i).setCellValue(value==null?"":String.valueOf(value));}}for(int i=0;i<headers.length;i++)sheet.setColumnWidth(i,Math.min(50,Math.max(12,headers[i].length()*2))*256);workbook.write(output);String name=URLEncoder.encode("系统操作日志.xlsx",StandardCharsets.UTF_8).replace("+","%20");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+name).contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(output.toByteArray());}}

    private String normalize(String value){return value==null||value.isBlank()?null:value.trim();}
    private String normalizeResult(String value){String result=normalize(value);if(result==null)return null;result=result.toUpperCase();if(!"SUCCESS".equals(result)&&!"FAILURE".equals(result))throw new IllegalArgumentException("日志结果只能为SUCCESS或FAILURE");return result;}
    private Map<String,Object> success(Object data){Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("data",data);return result;}
}
