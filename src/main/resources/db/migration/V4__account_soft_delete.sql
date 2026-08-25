ALTER TABLE ext_user_account_state
  ADD COLUMN previous_status VARCHAR(20) NULL AFTER account_status,
  ADD COLUMN deleted_by BIGINT NULL AFTER approved_at,
  ADD COLUMN deleted_at DATETIME NULL AFTER deleted_by,
  ADD KEY idx_account_state_deleted_by (deleted_by),
  ADD CONSTRAINT fk_account_state_deleted_by FOREIGN KEY (deleted_by) REFERENCES sys_user(user_id);
