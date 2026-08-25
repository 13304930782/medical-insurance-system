package com.medical.insurance.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemMapper {

    @Select("SELECT 1")
    Integer health();

    @Insert("INSERT INTO ext_operation_log "
        + "(user_id, operation_module, operation_type, business_no, operation_content, operation_result, ip_address) "
        + "VALUES (#{userId}, #{module}, #{operationType}, #{businessNo}, #{content}, #{result}, #{ipAddress})")
    int recordOperation(
        @Param("userId") Long userId,
        @Param("module") String module,
        @Param("operationType") String operationType,
        @Param("businessNo") String businessNo,
        @Param("content") String content,
        @Param("result") String result,
        @Param("ipAddress") String ipAddress
    );
}
