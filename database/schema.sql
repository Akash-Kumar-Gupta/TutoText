CREATE DATABASE IF NOT EXISTS tutotext_db
	CHARACTER SET utf8mb4
	COLLATE utf8mb4_unicode_ci;

USE tutotext_db;

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

CREATE TABLE IF NOT EXISTS transcriptions (
	id BIGINT NOT NULL AUTO_INCREMENT,
	title VARCHAR(150) NOT NULL,
	transcript_text LONGTEXT NOT NULL,
	language VARCHAR(30) NOT NULL,
	duration_seconds INT NOT NULL,
	owner_id BIGINT NOT NULL,
	created_at TIMESTAMP(6) NOT NULL,
	updated_at TIMESTAMP(6) NOT NULL,
	PRIMARY KEY (id),
	INDEX idx_transcriptions_created_at (created_at),
	INDEX idx_transcriptions_language (language),
	INDEX idx_transcriptions_owner_created_at (owner_id, created_at),
	CONSTRAINT fk_transcriptions_owner_id FOREIGN KEY (owner_id) REFERENCES app_users (id),
	CONSTRAINT chk_transcriptions_duration_nonnegative CHECK (duration_seconds >= 0)
) ENGINE=InnoDB
	DEFAULT CHARACTER SET utf8mb4
	COLLATE utf8mb4_unicode_ci;
