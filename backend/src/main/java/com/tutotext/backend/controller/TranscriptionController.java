package com.tutotext.backend.controller;

import com.tutotext.backend.dto.TranscriptionRequest;
import com.tutotext.backend.dto.TranscriptionResponse;
import com.tutotext.backend.service.TranscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {

	private final TranscriptionService service;

	public TranscriptionController(TranscriptionService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<TranscriptionResponse> create(
			@Valid @RequestBody TranscriptionRequest request,
			Authentication authentication) {
		TranscriptionResponse response = service.create(request, authentication.getName());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public List<TranscriptionResponse> findAll(Authentication authentication) {
		return service.findAll(authentication.getName());
	}

	@GetMapping("/{id}")
	public TranscriptionResponse findById(@PathVariable @Positive Long id, Authentication authentication) {
		return service.findById(id, authentication.getName());
	}

	@PutMapping("/{id}")
	public TranscriptionResponse update(
			@PathVariable @Positive Long id,
			@Valid @RequestBody TranscriptionRequest request,
			Authentication authentication) {
		return service.update(id, request, authentication.getName());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication authentication) {
		service.delete(id, authentication.getName());
		return ResponseEntity.noContent().build();
	}
}
