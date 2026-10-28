package com.tutotext.backend.service;

import com.tutotext.backend.dto.TranscriptionRequest;
import com.tutotext.backend.dto.TranscriptionResponse;
import com.tutotext.backend.entity.Transcription;
import com.tutotext.backend.exception.TranscriptionNotFoundException;
import com.tutotext.backend.repository.TranscriptionRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TranscriptionService {

	private final TranscriptionRepository repository;

	public TranscriptionService(TranscriptionRepository repository) {
		this.repository = repository;
	}

	public TranscriptionResponse create(TranscriptionRequest request) {
		Transcription transcription = new Transcription(
				request.title().trim(),
				request.transcriptText().trim(),
				request.language().trim(),
				request.durationSeconds());
		return TranscriptionResponse.from(repository.save(transcription));
	}

	@Transactional(readOnly = true)
	public List<TranscriptionResponse> findAll() {
		return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
				.stream()
				.map(TranscriptionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TranscriptionResponse findById(Long id) {
		return TranscriptionResponse.from(getTranscription(id));
	}

	public TranscriptionResponse update(Long id, TranscriptionRequest request) {
		Transcription transcription = getTranscription(id);
		transcription.setTitle(request.title().trim());
		transcription.setTranscriptText(request.transcriptText().trim());
		transcription.setLanguage(request.language().trim());
		transcription.setDurationSeconds(request.durationSeconds());
		return TranscriptionResponse.from(repository.saveAndFlush(transcription));
	}

	public void delete(Long id) {
		repository.delete(getTranscription(id));
	}

	private Transcription getTranscription(Long id) {
		return repository.findById(id).orElseThrow(() -> new TranscriptionNotFoundException(id));
	}
}
