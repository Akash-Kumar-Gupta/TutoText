-- Select the intended database explicitly in your MySQL client before running this migration.
-- Example: USE defaultdb;

CREATE TABLE IF NOT EXISTS app_users (
	id BIGINT NOT NULL AUTO_INCREMENT,
	email VARCHAR(254) NOT NULL,
	password_hash VARCHAR(60) NOT NULL,
	created_at TIMESTAMP(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_app_users_email UNIQUE (email)
) ENGINE=InnoDB
	DEFAULT CHARACTER SET utf8mb4
	COLLATE utf8mb4_unicode_ci;
