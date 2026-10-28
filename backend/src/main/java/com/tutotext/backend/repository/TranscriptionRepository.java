package com.tutotext.backend.repository;

import com.tutotext.backend.entity.Transcription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
}
