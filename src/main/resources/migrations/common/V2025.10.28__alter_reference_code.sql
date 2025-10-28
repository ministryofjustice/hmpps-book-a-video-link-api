ALTER TABLE reference_code ADD COLUMN display_sequence SMALLINT;

CREATE UNIQUE INDEX idx_reference_code_code_display_sequence ON reference_code (code, display_sequence);
