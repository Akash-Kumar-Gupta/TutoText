package com.tutotext.backend.controller;

import com.tutotext.backend.dto.AuthRequest;
import com.tutotext.backend.dto.AuthResponse;
import com.tutotext.backend.service.AuthService;
import com.tutotext.backend.service.RequestRateLimitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final RequestRateLimitService rateLimitService;

	public AuthController(AuthService authService, RequestRateLimitService rateLimitService) {
		this.authService = authService;
		this.rateLimitService = rateLimitService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
		rateLimitService.checkRegistrationEmail(request.email());
		AuthResponse response = authService.register(request);
		return ResponseEntity.status(201).body(response);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody AuthRequest request) {
		rateLimitService.checkLoginEmail(request.email());
		return authService.login(request);
	}
}
