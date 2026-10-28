package com.tutotext.backend.dto;

import com.tutotext.backend.entity.Transcription;
import java.time.Instant;

public record TranscriptionResponse(
		Long id,
		String title,
		String transcriptText,
		String language,
		Integer durationSeconds,
		Instant createdAt,
		Instant updatedAt) {

	public static TranscriptionResponse from(Transcription transcription) {
		return new TranscriptionResponse(
				transcription.getId(),
				transcription.getTitle(),
				transcription.getTranscriptText(),
				transcription.getLanguage(),
				transcription.getDurationSeconds(),
				transcription.getCreatedAt(),
				transcription.getUpdatedAt());
	}
}
