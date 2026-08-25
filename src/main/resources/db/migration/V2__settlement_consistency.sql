ALTER TABLE ext_reimbursement_settlement
  ADD COLUMN active_settlement_number VARCHAR(64)
    GENERATED ALWAYS AS (
      CASE
        WHEN transaction_type = 1 AND settlement_status = 'SETTLED'
        THEN hospitalization_number
        ELSE NULL
      END
    ) STORED,
  ADD UNIQUE KEY uk_active_settlement_visit (active_settlement_number);

CREATE INDEX idx_settlement_visit_status
  ON ext_reimbursement_settlement (hospitalization_number, settlement_status, transaction_type);
