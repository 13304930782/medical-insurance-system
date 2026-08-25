package com.medical.insurance.service.impl;

import com.medical.insurance.exception.BulkBusinessException;
import com.medical.insurance.model.BulkDeleteRequest;
import com.medical.insurance.service.BulkModuleRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.medical.insurance.service.impl.AuthService;
import com.medical.insurance.dao.SystemMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BulkDataService {
    private static final long MAX_FILE_SIZE=20L*1024*1024;
    private static final int MAX_ROWS=50_000;
    private final JdbcTemplate jdbc;
    private final AuthService authService;
    private final SystemMapper systemMapper;

    public BulkDataService(JdbcTemplate jdbc,AuthService authService,SystemMapper systemMapper){this.jdbc=jdbc;this.authService=authService;this.systemMapper=systemMapper;}

    public List<Map<String,Object>> modules(){
        return BulkModuleRegistry.all().stream().map(module->{
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("code",module.code());row.put("label",module.label());row.put("table",module.table());
            row.put("importable",module.importable());row.put("deletable",module.deletable());
            row.put("primaryKeys",schema(module).primaryKeys());
            return row;
        }).toList();
    }

    public byte[] template(String code){
        BulkModuleRegistry.Module module=BulkModuleRegistry.required(code);
        return workbook(module,List.of());
    }

    public byte[] export(String code,HttpServletRequest request){
        BulkModuleRegistry.Module module=BulkModuleRegistry.required(code);
        TableSchema schema=schema(module);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT "+quoted(schema.columnNames())+" FROM "+id(module.table())+" ORDER BY "+quoted(schema.primaryKeys()));
        byte[] bytes=workbook(module,rows);
        recordJob(module,"EXPORT",null,null,rows.size(),rows.size(),rows.size(),0,"COMPLETED",request);
        systemMapper.recordOperation(authService.currentUserId(request),"批量数据管理","EXPORT",module.code(),"导出"+module.label()+"："+rows.size()+"行","SUCCESS",request.getRemoteAddr());
        return bytes;
    }

    public Map<String,Object> importWorkbook(String code,String requestedMode,MultipartFile file,HttpServletRequest request){
        BulkModuleRegistry.Module module=BulkModuleRegistry.required(code);
        if(!module.importable())throw new BulkBusinessException(module.label()+"属于系统结算数据，只允许导出，不允许批量导入");
        String mode=normalizeMode(requestedMode);
        validateFile(file);
        byte[] bytes;
         try{bytes=file.getBytes();}catch(Exception e){throw new BulkBusinessException("无法读取上传文件");}
        String jobNo=jobNo();
        long jobId=createJob(jobNo,module,"IMPORT",mode,file.getOriginalFilename(),sha256(bytes),request);
        int total=0,success=0;
        List<Map<String,Object>> errors=new ArrayList<>();
         try(Workbook book=WorkbookFactory.create(new ByteArrayInputStream(bytes))){
            Sheet sheet=findSheet(book,module);
            if(sheet==null)throw new BulkBusinessException("Excel中未找到工作表："+module.table());
            TableSchema schema=schema(module);
            boolean legacy=legacyCatalogSheet(module,sheet);
            Row header=sheet.getRow(sheet.getFirstRowNum());
            List<Column> selected=legacy?legacyColumns(module,schema):readHeader(header,schema);
            requirePrimaryKeys(selected,schema);
            DataFormatter formatter=new DataFormatter(Locale.CHINA);
            for(int rowIndex=sheet.getFirstRowNum()+1;rowIndex<=sheet.getLastRowNum();rowIndex++){
                Row row=sheet.getRow(rowIndex);
                if(blankRow(row,selected.size(),formatter))continue;
                total++;
                 if(total>MAX_ROWS){addError(jobId,rowIndex+1,null,"单次最多导入"+MAX_ROWS+"行",errors);break;}
                try{
                    List<Object> values=legacy?legacyValues(module,row):new ArrayList<>();
                    if(!legacy)for(int index=0;index<selected.size();index++)values.add(cellValue(row==null?null:row.getCell(index),selected.get(index),formatter));
                     for(String key:schema.primaryKeys()){
                        int position=indexOf(selected,key);
                        if(position<0||values.get(position)==null||String.valueOf(values.get(position)).isBlank())throw new IllegalArgumentException("主键字段不能为空："+key);
                    }
                    if(!"VALIDATE_ONLY".equals(mode))jdbc.update(upsertSql(module.table(),selected,schema,"UPSERT".equals(mode)),values.toArray());
                    success++;
                 }catch(Exception e){addError(jobId,rowIndex+1,null,rootMessage(e),errors);}
            }
         }catch(BulkBusinessException e){finishJob(jobId,total,success,total-success,"FAILED");throw e;}
         catch(Exception e){finishJob(jobId,total,success,total-success,"FAILED");throw new BulkBusinessException("Excel解析失败："+rootMessage(e));}
        int failure=total-success;
        String status=failure==0?"COMPLETED":"COMPLETED_WITH_ERRORS";
        finishJob(jobId,total,success,failure,status);
        systemMapper.recordOperation(authService.currentUserId(request),"批量数据管理","IMPORT",module.code(),"导入"+module.label()+"：成功"+success+"行，失败"+failure+"行","SUCCESS",request.getRemoteAddr());
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("jobNo",jobNo);result.put("mode",mode);result.put("totalRows",total);result.put("successRows",success);result.put("failureRows",failure);result.put("errors",errors);return result;
    }

    @Transactional
    public Map<String,Object> deleteRows(String code,BulkDeleteRequest body,HttpServletRequest request){
        BulkModuleRegistry.Module module=BulkModuleRegistry.required(code);
        if(!module.deletable())throw new BulkBusinessException(module.label()+"不能通过通用批量接口删除，请使用对应业务页面以执行关联校验");
        List<Map<String,Object>> keys=body==null?null:body.getKeys();
        if(keys==null||keys.isEmpty())throw new BulkBusinessException("未选择要删除的数据");
        if(keys.size()>5000)throw new BulkBusinessException("单次最多删除5000行");
        String confirmation="DELETE "+keys.size();
        if(body.getConfirmation()==null||!confirmation.equals(body.getConfirmation().trim()))throw new BulkBusinessException("确认文字不正确，请输入："+confirmation);
        TableSchema schema=schema(module);
        int affected=0;
        String where=schema.primaryKeys().stream().map(key->id(key)+"=?").reduce((a,b)->a+" AND "+b).orElseThrow();
         for(Map<String,Object> row:keys){
            Object[] values=schema.primaryKeys().stream().map(key->{Object value=row.get(key);if(value==null||String.valueOf(value).isBlank())throw new BulkBusinessException("缺少主键字段："+key);return value;}).toArray();
            affected+=jdbc.update("DELETE FROM "+id(module.table())+" WHERE "+where,values);
        }
        recordJob(module,"DELETE",null,null,keys.size(),keys.size(),affected,keys.size()-affected,"COMPLETED",request);
        systemMapper.recordOperation(authService.currentUserId(request),"批量数据管理","DELETE",module.code(),"批量删除"+module.label()+"："+affected+"行","SUCCESS",request.getRemoteAddr());
        return Map.of("requestedRows",keys.size(),"deletedRows",affected);
    }

    private byte[] workbook(BulkModuleRegistry.Module module,List<Map<String,Object>> rows){
        TableSchema schema=schema(module);
         try(Workbook book=new XSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            Sheet sheet=book.createSheet(safeSheetName(module.table()));
            var headerStyle=book.createCellStyle();var font=book.createFont();font.setBold(true);headerStyle.setFont(font);headerStyle.setFillForegroundColor((short)42);headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);headerStyle.setWrapText(true);
            Row header=sheet.createRow(0);
            for(int index=0;index<schema.columns().size();index++){Cell cell=header.createCell(index);cell.setCellValue(schema.columns().get(index).name());cell.setCellStyle(headerStyle);sheet.setColumnWidth(index,Math.min(40,Math.max(14,schema.columns().get(index).name().length()+4))*256);}
            int rowIndex=1;
             for(Map<String,Object> values:rows){Row row=sheet.createRow(rowIndex++);for(int columnIndex=0;columnIndex<schema.columns().size();columnIndex++)writeCell(row.createCell(columnIndex),values.get(schema.columns().get(columnIndex).name()));}
            sheet.createFreezePane(0,1);sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,rowIndex-1),0,Math.max(0,schema.columns().size()-1)));
            book.write(output);return output.toByteArray();
         }catch(Exception e){throw new BulkBusinessException("生成Excel失败："+rootMessage(e));}
    }

    private void writeCell(Cell cell,Object value){if(value==null)return;if(value instanceof Number number)cell.setCellValue(number.doubleValue());else if(value instanceof java.util.Date date)cell.setCellValue(date);else if(value instanceof LocalDate date)cell.setCellValue(date);else if(value instanceof LocalDateTime dateTime)cell.setCellValue(dateTime);else cell.setCellValue(String.valueOf(value));}
    private List<Column> readHeader(Row row,TableSchema schema){if(row==null)throw new BulkBusinessException("Excel第一行必须是数据库字段名");List<Column> result=new ArrayList<>();for(int index=0;index<row.getLastCellNum();index++){String value=new DataFormatter().formatCellValue(row.getCell(index)).trim();if(value.isEmpty())throw new BulkBusinessException("第"+(index+1)+"列表头为空");Column column=schema.byName().get(value.toLowerCase(Locale.ROOT));if(column==null)throw new BulkBusinessException("字段不属于原表"+schema.table()+"："+value);result.add(column);}if(result.isEmpty())throw new BulkBusinessException("Excel没有字段表头");return result;}
    private void requirePrimaryKeys(List<Column> selected,TableSchema schema){for(String key:schema.primaryKeys())if(indexOf(selected,key)<0)throw new BulkBusinessException("Excel缺少主键字段："+key);}
    private int indexOf(List<Column> columns,String name){for(int i=0;i<columns.size();i++)if(columns.get(i).name().equalsIgnoreCase(name))return i;return -1;}
    private Object cellValue(Cell cell,Column column,DataFormatter formatter){if(cell==null||cell.getCellType()==CellType.BLANK)return null;if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell))return new Timestamp(cell.getDateCellValue().getTime());String text=formatter.formatCellValue(cell).trim();if(text.isEmpty())return null;if(numeric(column.sqlType())){try{return new BigDecimal(cell.getCellType()==CellType.NUMERIC?BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString():text.replace(",",""));}catch(Exception e){throw new IllegalArgumentException(column.name()+"不是有效数字："+text);}}if(dateType(column.sqlType()))return text.replace('T',' ');if(cell.getCellType()==CellType.NUMERIC)return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();return text;}
    private boolean blankRow(Row row,int columns,DataFormatter formatter){if(row==null)return true;for(int i=0;i<columns;i++){Cell cell=row.getCell(i);if(cell!=null&&!formatter.formatCellValue(cell).trim().isEmpty())return false;}return true;}
    private String upsertSql(String table,List<Column> columns,TableSchema schema,boolean upsert){String names=columns.stream().map(column->id(column.name())).reduce((a,b)->a+","+b).orElseThrow();String marks=String.join(",",java.util.Collections.nCopies(columns.size(),"?"));String sql="INSERT INTO "+id(table)+" ("+names+") VALUES ("+marks+")";if(!upsert)return sql;List<Column> updates=columns.stream().filter(column->!schema.primaryKeys().contains(column.name())).toList();if(updates.isEmpty())return sql+" ON DUPLICATE KEY UPDATE "+id(schema.primaryKeys().get(0))+"="+id(schema.primaryKeys().get(0));return sql+" ON DUPLICATE KEY UPDATE "+updates.stream().map(column->id(column.name())+"=VALUES("+id(column.name())+")").reduce((a,b)->a+","+b).orElseThrow();}
    private TableSchema schema(BulkModuleRegistry.Module module){List<Column> columns=jdbc.query("SELECT column_name,data_type FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? ORDER BY ordinal_position",(rs,row)->new Column(rs.getString(1),sqlType(rs.getString(2))),module.table());if(columns.isEmpty())throw new BulkBusinessException("数据库不存在表："+module.table());List<String> primary=jdbc.queryForList("SELECT column_name FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name=? AND constraint_name='PRIMARY' ORDER BY ordinal_position",String.class,module.table());if(primary.isEmpty())throw new BulkBusinessException("数据表没有主键，禁止通用批量操作："+module.table());Map<String,Column> byName=new LinkedHashMap<>();columns.forEach(column->byName.put(column.name().toLowerCase(Locale.ROOT),column));return new TableSchema(module.table(),columns,primary,byName);}
    private int sqlType(String type){return switch(type.toLowerCase(Locale.ROOT)){case "tinyint","smallint","mediumint","int","integer"->Types.INTEGER;case "bigint"->Types.BIGINT;case "decimal","numeric"->Types.DECIMAL;case "float","double"->Types.DOUBLE;case "date"->Types.DATE;case "datetime","timestamp"->Types.TIMESTAMP;default->Types.VARCHAR;};}
    private boolean numeric(int type){return type==Types.INTEGER||type==Types.BIGINT||type==Types.DECIMAL||type==Types.DOUBLE;}
    private boolean dateType(int type){return type==Types.DATE||type==Types.TIMESTAMP;}
    private String quoted(List<String> names){return names.stream().map(this::id).reduce((a,b)->a+","+b).orElseThrow();}
    private String id(String value){return "`"+value.replace("`","")+"`";}
    private String safeSheetName(String value){return value.length()>31?value.substring(0,31):value;}
    private Sheet findSheet(Workbook workbook,BulkModuleRegistry.Module module){Sheet named=workbook.getSheet(module.table());if(named!=null)return named;String legacy=switch(module.code()){case "medicines"->"药品";case "diagnoses"->"诊疗项目";case "facilities"->"服务设施";default->null;};if(legacy!=null&&workbook.getSheet(legacy)!=null)return workbook.getSheet(legacy);return workbook.getNumberOfSheets()==1?workbook.getSheetAt(0):null;}
    private boolean legacyCatalogSheet(BulkModuleRegistry.Module module,Sheet sheet){return switch(module.code()){case "medicines"->"药品".equals(sheet.getSheetName());case "diagnoses"->"诊疗项目".equals(sheet.getSheetName());case "facilities"->"服务设施".equals(sheet.getSheetName());default->false;};}
    private List<Column> legacyColumns(BulkModuleRegistry.Module module,TableSchema schema){String[] names=switch(module.code()){case "medicines"->new String[]{"med_id","med_name","med_exp_type","med_exp_level","med_measurement","med_max_prize","med_approvalmark","med_hos_level","med_size","med_tradename","med_starttime","med_endtime","med_valid","med_specialmark"};case "diagnoses"->new String[]{"dia_id","dia_name","dia_exp_type","dia_exp_level","dia_max_prize","dia_starttime","dia_endtime","dia_valid","dia_hos_level","dia_approvalmark"};case "facilities"->new String[]{"ser_id","ser_name","ser_exp_type","ser_starttime","ser_endtime","ser_valid"};default->throw new BulkBusinessException("不支持的原始目录格式");};List<Column> result=new ArrayList<>();for(String name:names)result.add(schema.byName().get(name.toLowerCase(Locale.ROOT)));return result;}
    private List<Object> legacyValues(BulkModuleRegistry.Module module,Row row){if(row==null)throw new IllegalArgumentException("空行");return switch(module.code()){
        case "medicines"->java.util.Arrays.asList(requiredLegacy(row,1),requiredLegacy(row,2),legacyText(row,8),legacyText(row,9),legacyText(row,3),legacyDecimal(row,10),"不需要审批","所有医院",legacyText(row,4),legacyText(row,12),legacyDate(row,5),legacyDate(row,6),legacyText(row,7),legacyText(row,11));
        case "diagnoses"->java.util.Arrays.asList(requiredLegacy(row,1),requiredLegacy(row,2),legacyText(row,3),legacyText(row,4),BigDecimal.ZERO,legacyDate(row,5),legacyDate(row,6),legacyText(row,7),"所有医院","不需要审批");
        case "facilities"->java.util.Arrays.asList(requiredLegacy(row,1),requiredLegacy(row,2),legacyText(row,3),legacyDate(row,4),legacyDate(row,5),legacyText(row,6));
        default->throw new IllegalArgumentException("不支持的原始目录格式");
    };}
    private String requiredLegacy(Row row,int index){String value=legacyText(row,index);if(value==null)throw new IllegalArgumentException("第"+(index+1)+"列不能为空");return value;}
    private String legacyText(Row row,int index){Cell cell=row.getCell(index);if(cell==null||cell.getCellType()==CellType.BLANK)return null;if(cell.getCellType()==CellType.NUMERIC)return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();String value=new DataFormatter(Locale.CHINA).formatCellValue(cell).trim();return value.isEmpty()?null:value;}
    private BigDecimal legacyDecimal(Row row,int index){String value=legacyText(row,index);return value==null?BigDecimal.ZERO:new BigDecimal(value);}
    private Timestamp legacyDate(Row row,int index){Cell cell=row.getCell(index);if(cell==null||cell.getCellType()==CellType.BLANK)return null;if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isValidExcelDate(cell.getNumericCellValue()))return Timestamp.valueOf(DateUtil.getLocalDateTime(cell.getNumericCellValue()));String value=new DataFormatter(Locale.CHINA).formatCellValue(cell).trim().replace('T',' ');return value.isEmpty()?null:Timestamp.valueOf(value.length()==10?value+" 00:00:00":value);}
    private void validateFile(MultipartFile file){if(file==null||file.isEmpty())throw new BulkBusinessException("请选择Excel文件");String name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase(Locale.ROOT);if(!name.endsWith(".xls")&&!name.endsWith(".xlsx"))throw new BulkBusinessException("只支持.xls或.xlsx文件");if(file.getSize()>MAX_FILE_SIZE)throw new BulkBusinessException("文件不能超过20MB");}
    private String normalizeMode(String value){String mode=value==null?"UPSERT":value.trim().toUpperCase(Locale.ROOT);if(!List.of("INSERT_ONLY","UPSERT","VALIDATE_ONLY").contains(mode))throw new BulkBusinessException("导入模式只能是INSERT_ONLY、UPSERT或VALIDATE_ONLY");return mode;}
    private String jobNo(){return "BULK-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase(Locale.ROOT);}
    private long createJob(String no,BulkModuleRegistry.Module module,String action,String mode,String filename,String hash,HttpServletRequest request){jdbc.update("INSERT INTO ext_bulk_job(job_no,module_code,job_action,import_mode,original_filename,file_sha256,job_status,operator_id) VALUES (?,?,?,?,?,?,?,?)",no,module.code(),action,mode,filename,hash,"RUNNING",authService.currentUserId(request));return jdbc.queryForObject("SELECT job_id FROM ext_bulk_job WHERE job_no=?",Long.class,no);}
    private void recordJob(BulkModuleRegistry.Module module,String action,String mode,String filename,int total,int valid,int success,int failure,String status,HttpServletRequest request){String no=jobNo();jdbc.update("INSERT INTO ext_bulk_job(job_no,module_code,job_action,import_mode,original_filename,total_rows,valid_rows,success_rows,failure_rows,job_status,operator_id,completed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW())",no,module.code(),action,mode,filename,total,valid,success,failure,status,authService.currentUserId(request));}
    private void finishJob(long id,int total,int success,int failure,String status){jdbc.update("UPDATE ext_bulk_job SET total_rows=?,valid_rows=?,success_rows=?,failure_rows=?,job_status=?,completed_at=NOW() WHERE job_id=?",total,success,success,failure,status,id);}
    private void addError(long jobId,int row,String field,String message,List<Map<String,Object>> errors){String value=message==null?"未知错误":message;if(value.length()>1000)value=value.substring(0,1000);jdbc.update("INSERT INTO ext_bulk_job_error(job_id,`row_number`,field_name,error_message) VALUES (?,?,?,?)",jobId,row,field,value);if(errors.size()<100)errors.add(Map.of("rowNumber",row,"field",field==null?"":field,"message",value));}
    private String sha256(byte[] data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(Exception e){throw new IllegalStateException(e);}}
    private String rootMessage(Throwable error){Throwable current=error;while(current.getCause()!=null)current=current.getCause();String value=current.getMessage();return value==null?current.getClass().getSimpleName():value;}
    private record Column(String name,int sqlType){}
    private record TableSchema(String table,List<Column> columns,List<String> primaryKeys,Map<String,Column> byName){List<String> columnNames(){return columns.stream().map(Column::name).toList();}}
}
