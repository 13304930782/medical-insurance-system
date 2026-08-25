package com.medical.insurance.dao;

import com.medical.insurance.model.SystemUser;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT user_id, username, password_hash, real_name, role_code, enabled "
        + "FROM sys_user WHERE username = #{username}")
    @ConstructorArgs({
        @Arg(column = "user_id", javaType = long.class, id = true),
        @Arg(column = "username", javaType = String.class),
        @Arg(column = "password_hash", javaType = String.class),
        @Arg(column = "real_name", javaType = String.class),
        @Arg(column = "role_code", javaType = String.class),
        @Arg(column = "enabled", javaType = boolean.class)
    })
    SystemUser findByUsername(@Param("username") String username);

    @Select("SELECT u.user_id,u.username,u.password_hash,u.real_name,u.role_code,u.enabled FROM sys_user u LEFT JOIN ext_user_security s ON s.user_id=u.user_id WHERE u.username=#{identifier} OR LOWER(s.email)=LOWER(#{identifier}) LIMIT 1")
    @ConstructorArgs({
        @Arg(column="user_id",javaType=long.class,id=true),@Arg(column="username",javaType=String.class),
        @Arg(column="password_hash",javaType=String.class),@Arg(column="real_name",javaType=String.class),
        @Arg(column="role_code",javaType=String.class),@Arg(column="enabled",javaType=boolean.class)
    })
    SystemUser findByLoginIdentifier(@Param("identifier")String identifier);

    @Select("SELECT user_id,username,password_hash,real_name,role_code,enabled FROM sys_user WHERE user_id=#{userId}")
    @ConstructorArgs({
        @Arg(column="user_id",javaType=long.class,id=true),@Arg(column="username",javaType=String.class),
        @Arg(column="password_hash",javaType=String.class),@Arg(column="real_name",javaType=String.class),
        @Arg(column="role_code",javaType=String.class),@Arg(column="enabled",javaType=boolean.class)
    })
    SystemUser findById(@Param("userId")long userId);

    @Select("SELECT u.user_id,u.username,u.password_hash,u.real_name,u.role_code,u.enabled FROM sys_user u JOIN ext_user_security s ON s.user_id=u.user_id WHERE u.username=#{username} AND LOWER(s.email)=LOWER(#{email})")
    @ConstructorArgs({
        @Arg(column="user_id",javaType=long.class,id=true),@Arg(column="username",javaType=String.class),
        @Arg(column="password_hash",javaType=String.class),@Arg(column="real_name",javaType=String.class),
        @Arg(column="role_code",javaType=String.class),@Arg(column="enabled",javaType=boolean.class)
    })
    SystemUser findByUsernameAndEmail(@Param("username")String username,@Param("email")String email);

    @Insert("INSERT INTO sys_user (username, password_hash, real_name, role_code, enabled) "
        + "VALUES (#{username}, #{passwordHash}, #{realName}, #{roleCode}, 1)")
    int insert(
        @Param("username") String username,
        @Param("passwordHash") String passwordHash,
        @Param("realName") String realName,
        @Param("roleCode") String roleCode
    );

    @Insert("INSERT INTO sys_user (username,password_hash,real_name,role_code,enabled) VALUES (#{username},#{passwordHash},#{realName},'REIMBURSEMENT',0)")
    int insertPending(@Param("username")String username,@Param("passwordHash")String passwordHash,@Param("realName")String realName);

    @Insert("INSERT IGNORE INTO ext_user_security(user_id,email) VALUES(#{userId},#{email})")
    int insertSecurity(@Param("userId")long userId,@Param("email")String email);

    @Insert("INSERT INTO ext_user_security(user_id,email) VALUES(#{userId},#{email})")
    int insertSecurityWithEmail(@Param("userId")long userId,@Param("email")String email);

    @Insert("INSERT INTO ext_user_security(user_id,email) VALUES(#{userId},#{email}) ON DUPLICATE KEY UPDATE email=COALESCE(email,VALUES(email))")
    int ensureSecurityEmail(@Param("userId")long userId,@Param("email")String email);

    @Insert("INSERT IGNORE INTO ext_user_account_state(user_id,account_status,session_version,approved_at) VALUES(#{userId},'APPROVED',1,NOW())")
    int ensureApprovedState(@Param("userId")long userId);

    @Insert("INSERT INTO ext_user_account_state(user_id,account_status,session_version) VALUES(#{userId},'PENDING',1)")
    int insertPendingState(@Param("userId")long userId);

    @Select("SELECT account_status AS accountStatus,session_version AS sessionVersion,approved_by AS approvedBy,approved_at AS approvedAt FROM ext_user_account_state WHERE user_id=#{userId}")
    Map<String,Object> accountState(@Param("userId")long userId);

    @Select("SELECT u.user_id AS userId,u.username,u.real_name AS realName,u.role_code AS roleCode,u.enabled,s.email,a.account_status AS accountStatus,a.created_at AS registeredAt,a.approved_at AS approvedAt,a.deleted_at AS deletedAt FROM sys_user u JOIN ext_user_security s ON s.user_id=u.user_id JOIN ext_user_account_state a ON a.user_id=u.user_id WHERE ((#{status} IS NULL AND a.account_status!='DELETED') OR a.account_status=#{status}) ORDER BY a.created_at DESC,u.user_id DESC")
    List<Map<String,Object>> accounts(@Param("status")String status);

    @Update("UPDATE ext_user_account_state SET account_status='APPROVED',approved_by=#{approverId},approved_at=NOW(),session_version=session_version+1 WHERE user_id=#{userId} AND account_status='PENDING'")
    int approveAccountState(@Param("userId")long userId,@Param("approverId")long approverId);

    @Update("UPDATE sys_user SET role_code=#{roleCode},enabled=1 WHERE user_id=#{userId}")
    int enableApprovedAccount(@Param("userId")long userId,@Param("roleCode")String roleCode);

    @Update("UPDATE ext_user_account_state SET account_status='REJECTED',approved_by=#{approverId},approved_at=NOW(),session_version=session_version+1 WHERE user_id=#{userId} AND account_status='PENDING'")
    int rejectAccountState(@Param("userId")long userId,@Param("approverId")long approverId);

    @Update("UPDATE ext_user_account_state SET session_version=session_version+1 WHERE user_id=#{userId}")
    int bumpSessionVersion(@Param("userId")long userId);

    @Update("UPDATE ext_user_account_state SET previous_status=account_status,account_status='DELETED',deleted_by=#{deletedBy},deleted_at=NOW(),session_version=session_version+1 WHERE user_id=#{userId} AND account_status!='DELETED'")
    int softDeleteAccount(@Param("userId")long userId,@Param("deletedBy")long deletedBy);

    @Update("UPDATE ext_user_account_state SET account_status=CASE WHEN previous_status IN ('PENDING','APPROVED','REJECTED') THEN previous_status ELSE 'REJECTED' END,previous_status=NULL,deleted_by=NULL,deleted_at=NULL,session_version=session_version+1 WHERE user_id=#{userId} AND account_status='DELETED'")
    int restoreDeletedAccount(@Param("userId")long userId);

    @Update("UPDATE sys_user SET enabled=#{enabled} WHERE user_id=#{userId}")
    int setAccountEnabled(@Param("userId")long userId,@Param("enabled")boolean enabled);

    @Select("SELECT email,failed_attempts AS failedAttempts,locked_until AS lockedUntil FROM ext_user_security WHERE user_id=#{userId}")
    Map<String,Object> security(@Param("userId")long userId);

    @Update("UPDATE ext_user_security SET failed_attempts=failed_attempts+1,locked_until=CASE WHEN failed_attempts+1>=5 THEN DATE_ADD(NOW(),INTERVAL 15 MINUTE) ELSE locked_until END WHERE user_id=#{userId}")
    int recordLoginFailure(@Param("userId")long userId);

    @Update("UPDATE ext_user_security SET failed_attempts=0,locked_until=NULL WHERE user_id=#{userId}")
    int clearLoginFailures(@Param("userId")long userId);

    @Update("UPDATE sys_user SET password_hash=#{passwordHash} WHERE user_id=#{userId}")
    int updatePassword(@Param("userId")long userId,@Param("passwordHash")String passwordHash);

    @Update("UPDATE ext_user_security SET password_changed_at=NOW(),failed_attempts=0,locked_until=NULL WHERE user_id=#{userId}")
    int recordPasswordChanged(@Param("userId")long userId);

    @Update("UPDATE ext_password_reset_token SET used_at=NOW() WHERE user_id=#{userId} AND used_at IS NULL")
    int invalidateResetTokens(@Param("userId")long userId);

    @Select("SELECT user_id FROM ext_user_security WHERE user_id=#{userId} FOR UPDATE")
    Long lockPasswordReset(@Param("userId")long userId);

    @Select("SELECT GREATEST(0,60-TIMESTAMPDIFF(SECOND,created_at,NOW())) FROM ext_password_reset_token WHERE user_id=#{userId} ORDER BY token_id DESC LIMIT 1 FOR UPDATE")
    Integer passwordResetRetryAfterSeconds(@Param("userId")long userId);

    @Insert("INSERT INTO ext_password_reset_token(user_id,token_hash,expires_at) VALUES(#{userId},#{tokenHash},#{expiresAt})")
    int insertResetToken(@Param("userId")long userId,@Param("tokenHash")String tokenHash,@Param("expiresAt")LocalDateTime expiresAt);

    @Select("SELECT token_id AS tokenId,token_hash AS tokenHash,expires_at AS expiresAt,failed_attempts AS failedAttempts FROM ext_password_reset_token WHERE user_id=#{userId} AND used_at IS NULL ORDER BY token_id DESC LIMIT 1")
    Map<String,Object> latestResetToken(@Param("userId")long userId);

    @Update("UPDATE ext_password_reset_token SET failed_attempts=failed_attempts+1,used_at=CASE WHEN failed_attempts+1>=5 THEN NOW() ELSE used_at END WHERE token_id=#{tokenId}")
    int recordResetFailure(@Param("tokenId")long tokenId);

    @Update("UPDATE ext_password_reset_token SET used_at=NOW() WHERE token_id=#{tokenId}")
    int consumeResetToken(@Param("tokenId")long tokenId);
}
