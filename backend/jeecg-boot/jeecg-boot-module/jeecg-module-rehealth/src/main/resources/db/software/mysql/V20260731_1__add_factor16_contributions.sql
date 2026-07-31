ALTER TABLE rehealth_cvd_risk_result
    ADD COLUMN factor_contribution_version VARCHAR(64) NULL AFTER contribution_method,
    ADD COLUMN factor_contribution_json LONGTEXT NULL AFTER contribution_json,
    ADD COLUMN factor_measured_component_json LONGTEXT NULL AFTER factor_contribution_json,
    ADD COLUMN factor_control_support_json LONGTEXT NULL AFTER factor_measured_component_json;
