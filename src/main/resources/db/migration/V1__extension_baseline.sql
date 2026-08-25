CREATE TABLE IF NOT EXISTS ext_user_security (
  user_id BIGINT NOT NULL,
  email VARCHAR(120) NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  password_changed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_security_email (email),
  CONSTRAINT fk_user_security_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户安全扩展表';

CREATE TABLE IF NOT EXISTS ext_user_account_state (
  user_id BIGINT NOT NULL,
  account_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  session_version BIGINT NOT NULL DEFAULT 1,
  approved_by BIGINT NULL,
  approved_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  KEY idx_account_state_status (account_status),
  CONSTRAINT fk_account_state_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_account_state_approver FOREIGN KEY (approved_by) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账号审核及会话版本扩展表';

CREATE TABLE IF NOT EXISTS ext_password_reset_token (
  token_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (token_id),
  KEY idx_reset_user_status (user_id,used_at,expires_at),
  CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置验证码扩展表';

CREATE TABLE IF NOT EXISTS ext_business_sequence (
  sequence_key VARCHAR(40) NOT NULL,
  current_value BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sequence_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务编号并发序列表';

CREATE TABLE IF NOT EXISTS ext_dictionary_item (
  category VARCHAR(80) NOT NULL,
  item_value VARCHAR(120) NOT NULL,
  item_label VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (category,item_value),
  KEY idx_ext_dictionary_enabled (category,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始参数之外的扩展字典';

CREATE TABLE IF NOT EXISTS ext_operation_log_detail (
  log_id BIGINT NOT NULL,
  request_id VARCHAR(40) NOT NULL,
  request_method VARCHAR(10) NOT NULL,
  request_path VARCHAR(500) NOT NULL,
  http_status INT NOT NULL,
  duration_ms BIGINT NOT NULL DEFAULT 0,
  user_agent VARCHAR(500) NULL,
  affected_rows INT NULL,
  detail_json JSON NULL,
  PRIMARY KEY (log_id),
  UNIQUE KEY uk_operation_request (request_id),
  KEY idx_operation_path_time (request_path,http_status),
  CONSTRAINT fk_operation_detail_log FOREIGN KEY (log_id) REFERENCES ext_operation_log(log_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志请求明细';

CREATE TABLE IF NOT EXISTS ext_bulk_job (
  job_id BIGINT NOT NULL AUTO_INCREMENT,
  job_no VARCHAR(40) NOT NULL,
  module_code VARCHAR(40) NOT NULL,
  job_action VARCHAR(20) NOT NULL,
  import_mode VARCHAR(20) NULL,
  original_filename VARCHAR(255) NULL,
  file_sha256 CHAR(64) NULL,
  total_rows INT NOT NULL DEFAULT 0,
  valid_rows INT NOT NULL DEFAULT 0,
  success_rows INT NOT NULL DEFAULT 0,
  failure_rows INT NOT NULL DEFAULT 0,
  job_status VARCHAR(20) NOT NULL,
  operator_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  PRIMARY KEY (job_id),
  UNIQUE KEY uk_bulk_job_no (job_no),
  KEY idx_bulk_module_time (module_code,created_at),
  CONSTRAINT fk_bulk_job_operator FOREIGN KEY (operator_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量导入导出任务';

CREATE TABLE IF NOT EXISTS ext_bulk_job_error (
  error_id BIGINT NOT NULL AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  `row_number` INT NOT NULL,
  field_name VARCHAR(80) NULL,
  error_message VARCHAR(1000) NOT NULL,
  PRIMARY KEY (error_id),
  KEY idx_bulk_error_job_row (job_id,`row_number`),
  CONSTRAINT fk_bulk_error_job FOREIGN KEY (job_id) REFERENCES ext_bulk_job(job_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量任务错误明细';

CREATE TABLE IF NOT EXISTS ext_ai_knowledge_document (
  document_id BIGINT NOT NULL AUTO_INCREMENT,
  source_type VARCHAR(20) NOT NULL,
  source_url VARCHAR(1000) NULL,
  title VARCHAR(500) NOT NULL,
  publisher VARCHAR(200) NULL,
  published_at DATETIME NULL,
  fetched_at DATETIME NULL,
  content_hash CHAR(64) NOT NULL,
  document_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (document_id),
  UNIQUE KEY uk_ai_document_hash (content_hash),
  KEY idx_ai_document_status (document_status,source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答官方资料';

CREATE TABLE IF NOT EXISTS ext_ai_knowledge_chunk (
  chunk_id BIGINT NOT NULL AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  chunk_content LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chunk_id),
  UNIQUE KEY uk_ai_chunk_document (document_id,chunk_index),
  FULLTEXT KEY ft_ai_chunk_content (chunk_content) WITH PARSER ngram,
  CONSTRAINT fk_ai_chunk_document FOREIGN KEY (document_id) REFERENCES ext_ai_knowledge_document(document_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答资料分块';

CREATE TABLE IF NOT EXISTS ext_ai_query_log (
  query_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id VARCHAR(40) NOT NULL,
  user_id BIGINT NULL,
  question_masked VARCHAR(2000) NOT NULL,
  answer_text LONGTEXT NULL,
  evidence_sufficient TINYINT(1) NOT NULL DEFAULT 0,
  source_ids JSON NULL,
  model_name VARCHAR(80) NULL,
  prompt_version VARCHAR(40) NOT NULL,
  duration_ms BIGINT NOT NULL DEFAULT 0,
  query_result VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (query_id),
  UNIQUE KEY uk_ai_query_request (request_id),
  KEY idx_ai_query_user_time (user_id,created_at),
  CONSTRAINT fk_ai_query_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI政策问答留痕';

INSERT IGNORE INTO ext_user_security(user_id,email)
SELECT user_id,NULL FROM sys_user;

INSERT IGNORE INTO ext_user_account_state(user_id,account_status,approved_at)
SELECT user_id,'APPROVED',CURRENT_TIMESTAMP FROM sys_user;

INSERT IGNORE INTO ext_business_sequence(sequence_key,current_value)
VALUES ('OUTPATIENT',0);
