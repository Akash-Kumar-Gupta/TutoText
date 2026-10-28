package com.tutotext.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutotext.backend.dto.TranscriptionRequest;
import com.tutotext.backend.dto.TranscriptionResponse;
import com.tutotext.backend.entity.Transcription;
import com.tutotext.backend.exception.TranscriptionNotFoundException;
import com.tutotext.backend.repository.TranscriptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

	@Mock
	private TranscriptionRepository repository;

	@InjectMocks
	private TranscriptionService service;

	@Test
	void createTrimsTextFieldsAndSavesTranscription() {
		when(repository.save(any(Transcription.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TranscriptionResponse response = service.create(
				new TranscriptionRequest("  Meeting  ", "  Hello there  ", "  English  ", 45));

		assertEquals("Meeting", response.title());
		assertEquals("Hello there", response.transcriptText());
		assertEquals("English", response.language());
		assertEquals(45, response.durationSeconds());
		verify(repository).save(any(Transcription.class));
	}

	@Test
	void findByIdThrowsWhenTranscriptionDoesNotExist() {
		when(repository.findById(42L)).thenReturn(Optional.empty());

		TranscriptionNotFoundException exception = assertThrows(
				TranscriptionNotFoundException.class,
				() -> service.findById(42L));

		assertEquals("Transcription with id 42 was not found", exception.getMessage());
	}

	@Test
	void deleteRemovesExistingTranscription() {
		Transcription transcription = new Transcription("Title", "Text", "English", 10);
		when(repository.findById(1L)).thenReturn(Optional.of(transcription));

		service.delete(1L);

		verify(repository).delete(transcription);
	}
}
