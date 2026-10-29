package com.tutotext.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
@Table(name = "transcriptions", indexes = {
		@Index(name = "idx_transcriptions_created_at", columnList = "created_at"),
		@Index(name = "idx_transcriptions_language", columnList = "language"),
		@Index(name = "idx_transcriptions_owner_created_at", columnList = "owner_id, created_at")
})
public class Transcription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(name = "transcript_text", nullable = false, columnDefinition = "LONGTEXT")
	private String transcriptText;

	@Column(nullable = false, length = 30)
	private String language;

	@Column(name = "duration_seconds", nullable = false)
	private Integer durationSeconds;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private AppUser owner;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Transcription() {
	}

	public Transcription(String title, String transcriptText, String language, Integer durationSeconds, AppUser owner) {
		this.title = title;
		this.transcriptText = transcriptText;
		this.language = language;
		this.durationSeconds = durationSeconds;
		this.owner = owner;
	}

	@PrePersist
	void setCreationTimestamps() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void setUpdatedTimestamp() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTranscriptText() {
		return transcriptText;
	}

	public void setTranscriptText(String transcriptText) {
		this.transcriptText = transcriptText;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public AppUser getOwner() {
		return owner;
	}

	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
