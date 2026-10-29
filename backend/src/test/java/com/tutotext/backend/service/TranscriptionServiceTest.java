package com.tutotext.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutotext.backend.dto.TranscriptionRequest;
import com.tutotext.backend.dto.TranscriptionResponse;
import com.tutotext.backend.entity.AppUser;
import com.tutotext.backend.entity.Transcription;
import com.tutotext.backend.exception.TranscriptionNotFoundException;
import com.tutotext.backend.repository.AppUserRepository;
import com.tutotext.backend.repository.TranscriptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

	@Mock
	private TranscriptionRepository repository;

	@Mock
	private AppUserRepository userRepository;

	private TranscriptionService service;
	private AppUser owner;

	@BeforeEach
	void setUp() {
		service = new TranscriptionService(repository, userRepository);
		owner = org.mockito.Mockito.mock(AppUser.class);
		when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
	}

	@Test
	void createTrimsTextFieldsAndSavesTranscription() {
		when(repository.save(any(Transcription.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TranscriptionResponse response = service.create(
				new TranscriptionRequest("  Meeting  ", "  Hello there  ", "  English  ", 45),
				"owner@example.com");

		assertEquals("Meeting", response.title());
		assertEquals("Hello there", response.transcriptText());
		assertEquals("English", response.language());
		assertEquals(45, response.durationSeconds());
		org.mockito.ArgumentCaptor<Transcription> saved = org.mockito.ArgumentCaptor.forClass(Transcription.class);
		verify(repository).save(saved.capture());
		assertEquals(owner, saved.getValue().getOwner());
	}

	@Test
	void findByIdThrowsWhenTranscriptionDoesNotExist() {
		when(owner.getId()).thenReturn(7L);
		when(repository.findByIdAndOwner_Id(42L, 7L)).thenReturn(Optional.empty());

		TranscriptionNotFoundException exception = assertThrows(
				TranscriptionNotFoundException.class,
				() -> service.findById(42L, "owner@example.com"));

		assertEquals("Transcription with id 42 was not found", exception.getMessage());
	}

	@Test
	void deleteRemovesExistingTranscription() {
		when(owner.getId()).thenReturn(7L);
		Transcription transcription = new Transcription("Title", "Text", "English", 10, owner);
		when(repository.findByIdAndOwner_Id(1L, 7L)).thenReturn(Optional.of(transcription));

		service.delete(1L, "owner@example.com");

		verify(repository).delete(transcription);
	}
}
