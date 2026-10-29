package com.tutotext.backend.service;

import com.tutotext.backend.dto.TranscriptionRequest;
import com.tutotext.backend.dto.TranscriptionResponse;
import com.tutotext.backend.entity.AppUser;
import com.tutotext.backend.entity.Transcription;
import com.tutotext.backend.exception.TranscriptionNotFoundException;
import com.tutotext.backend.repository.AppUserRepository;
import com.tutotext.backend.repository.TranscriptionRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TranscriptionService {

	private final TranscriptionRepository repository;
	private final AppUserRepository userRepository;

	public TranscriptionService(TranscriptionRepository repository, AppUserRepository userRepository) {
		this.repository = repository;
		this.userRepository = userRepository;
	}

	public TranscriptionResponse create(TranscriptionRequest request, String authenticatedEmail) {
		Transcription transcription = new Transcription(
				request.title().trim(),
				request.transcriptText().trim(),
				request.language().trim(),
				request.durationSeconds(),
				getCurrentUser(authenticatedEmail));
		return TranscriptionResponse.from(repository.save(transcription));
	}

	@Transactional(readOnly = true)
	public List<TranscriptionResponse> findAll(String authenticatedEmail) {
		AppUser owner = getCurrentUser(authenticatedEmail);
		return repository.findAllByOwner_Id(owner.getId(), Sort.by(Sort.Direction.DESC, "createdAt"))
				.stream()
				.map(TranscriptionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TranscriptionResponse findById(Long id, String authenticatedEmail) {
		return TranscriptionResponse.from(getTranscription(id, authenticatedEmail));
	}

	public TranscriptionResponse update(Long id, TranscriptionRequest request, String authenticatedEmail) {
		Transcription transcription = getTranscription(id, authenticatedEmail);
		transcription.setTitle(request.title().trim());
		transcription.setTranscriptText(request.transcriptText().trim());
		transcription.setLanguage(request.language().trim());
		transcription.setDurationSeconds(request.durationSeconds());
		return TranscriptionResponse.from(repository.saveAndFlush(transcription));
	}

	public void delete(Long id, String authenticatedEmail) {
		repository.delete(getTranscription(id, authenticatedEmail));
	}

	private Transcription getTranscription(Long id, String authenticatedEmail) {
		AppUser owner = getCurrentUser(authenticatedEmail);
		return repository.findByIdAndOwner_Id(id, owner.getId())
				.orElseThrow(() -> new TranscriptionNotFoundException(id));
	}

	private AppUser getCurrentUser(String authenticatedEmail) {
		return userRepository.findByEmail(authenticatedEmail)
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists"));
	}
}
