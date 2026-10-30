-- Select the intended database explicitly in your MySQL client before running this migration.
-- Example: USE defaultdb;

ALTER TABLE transcriptions
	ADD COLUMN owner_id BIGINT NULL,
	ADD INDEX idx_transcriptions_owner_created_at (owner_id, created_at),
	ADD CONSTRAINT fk_transcriptions_owner_id
		FOREIGN KEY (owner_id) REFERENCES app_users (id);
