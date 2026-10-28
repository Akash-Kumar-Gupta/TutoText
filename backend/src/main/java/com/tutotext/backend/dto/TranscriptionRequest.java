package com.tutotext.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TranscriptionRequest(
		@NotBlank @Size(max = 150) String title,
		@NotBlank @Size(max = 4_000_000) String transcriptText,
		@NotBlank @Size(max = 30) String language,
		@NotNull @PositiveOrZero Integer durationSeconds) {
}
