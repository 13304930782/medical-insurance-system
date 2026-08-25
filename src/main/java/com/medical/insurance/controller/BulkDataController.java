package com.medical.insurance.controller;

import com.medical.insurance.exception.BulkBusinessException;
import com.medical.insurance.model.BulkDeleteRequest;
import com.medical.insurance.service.impl.BulkDataService;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bulk")
public class BulkDataController {
    private static final MediaType XLSX=MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final BulkDataService service;
    BulkDataController(BulkDataService service){this.service=service;}

    @GetMapping("/modules") Map<String,Object> modules(){return success(service.modules());}
    @GetMapping("/{module}/template.xlsx") ResponseEntity<byte[]> template(@PathVariable String module){return file(service.template(module),module+"-template.xlsx");}
    @GetMapping("/{module}/export.xlsx") ResponseEntity<byte[]> export(@PathVariable String module,HttpServletRequest request){return file(service.export(module,request),module+"-export.xlsx");}
    @PostMapping(value="/{module}/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Map<String,Object> importWorkbook(@PathVariable String module,@RequestParam(defaultValue="UPSERT")String mode,@RequestParam("file")MultipartFile file,HttpServletRequest request){return success(service.importWorkbook(module,mode,file,request));}
    @DeleteMapping("/{module}") Map<String,Object> delete(@PathVariable String module,@RequestBody BulkDeleteRequest body,HttpServletRequest request){return success(service.deleteRows(module,body,request));}

    @ExceptionHandler(BulkBusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> error(BulkBusinessException e){Map<String,Object> result=new LinkedHashMap<>();result.put("success",false);result.put("message",e.getMessage());return result;}
    private ResponseEntity<byte[]> file(byte[] body,String filename){HttpHeaders headers=new HttpHeaders();headers.setContentType(XLSX);headers.setContentDisposition(ContentDisposition.attachment().filename(filename,StandardCharsets.UTF_8).build());return new ResponseEntity<>(body,headers,HttpStatus.OK);}
    private Map<String,Object> success(Object data){Map<String,Object> result=new LinkedHashMap<>();result.put("success",true);result.put("data",data);return result;}
}
