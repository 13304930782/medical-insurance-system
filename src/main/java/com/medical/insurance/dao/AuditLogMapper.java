package com.medical.insurance.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditLogMapper {
    String ACTION_LABEL="CASE l.operation_type WHEN 'CREATE' THEN '新增' WHEN 'UPDATE' THEN '修改' WHEN 'DELETE' THEN '删除' WHEN 'RESTORE' THEN '恢复账号' WHEN 'LOGIN' THEN '登录系统' WHEN 'LOGOUT' THEN '退出系统' WHEN 'REGISTER' THEN '注册账号' WHEN 'RESET_REQUEST' THEN '申请找回验证码' WHEN 'RESET_PASSWORD' THEN '重置密码' WHEN 'CHANGE_PASSWORD' THEN '修改密码' WHEN 'APPROVE' THEN '审核通过' WHEN 'REJECT' THEN '审核拒绝' WHEN 'IMPORT' THEN '导入' WHEN 'EXPORT' THEN '导出' WHEN 'SETTLE' THEN '正式结算' WHEN 'CANCEL' THEN '取消报销' ELSE l.operation_type END";
    String FILTER="<if test='keyword != null'> AND (l.operation_module LIKE CONCAT('%',#{keyword},'%') OR "+ACTION_LABEL+" LIKE CONCAT('%',#{keyword},'%') OR l.business_no LIKE CONCAT('%',#{keyword},'%') OR l.operation_content LIKE CONCAT('%',#{keyword},'%') OR u.username LIKE CONCAT('%',#{keyword},'%') OR u.real_name LIKE CONCAT('%',#{keyword},'%'))</if><if test='result != null'> AND l.operation_result=#{result}</if>";
    String COLUMNS="l.log_id AS logId,l.user_id AS userId,u.username,u.real_name AS realName,l.operation_module AS operationModule,l.operation_type AS operationType,"+ACTION_LABEL+" AS operationLabel,l.business_no AS businessNo,l.operation_content AS operationContent,l.operation_result AS operationResult,l.ip_address AS ipAddress,l.created_at AS createdAt";
    String FROM=" FROM ext_operation_log l LEFT JOIN sys_user u ON u.user_id=l.user_id WHERE l.operation_module!='HTTP接口审计' ";

    @Select("<script>SELECT COUNT(*)"+FROM+FILTER+"</script>")
    long count(@Param("keyword")String keyword,@Param("result")String result);

    @Select("<script>SELECT "+COLUMNS+FROM+FILTER+" ORDER BY l.created_at DESC,l.log_id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String,Object>> page(@Param("keyword")String keyword,@Param("result")String result,@Param("offset")int offset,@Param("size")int size);

    @Select("<script>SELECT "+COLUMNS+FROM+FILTER+" ORDER BY l.created_at DESC,l.log_id DESC LIMIT 50000</script>")
    List<Map<String,Object>> export(@Param("keyword")String keyword,@Param("result")String result);
}
