package com.tutotext.backend.repository;

import com.tutotext.backend.entity.Transcription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
	List<Transcription> findAllByOwner_Id(Long ownerId, Sort sort);

	Optional<Transcription> findByIdAndOwner_Id(Long id, Long ownerId);
}
