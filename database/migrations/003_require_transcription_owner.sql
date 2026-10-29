USE tutotext_db;

ALTER TABLE transcriptions
	MODIFY COLUMN owner_id BIGINT NOT NULL;
