-- Select the intended database explicitly in your MySQL client before running this migration.
-- Example: USE defaultdb;

ALTER TABLE transcriptions
	MODIFY COLUMN owner_id BIGINT NOT NULL;
