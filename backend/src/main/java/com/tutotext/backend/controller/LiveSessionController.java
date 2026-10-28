package com.tutotext.backend.controller;

import com.tutotext.backend.dto.LiveSessionTokenResponse;
import com.tutotext.backend.service.GeminiLiveTokenService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live-sessions")
public class LiveSessionController {

	private final GeminiLiveTokenService tokenService;

	public LiveSessionController(GeminiLiveTokenService tokenService) {
		this.tokenService = tokenService;
	}

	@PostMapping("/token")
	public LiveSessionTokenResponse createToken() {
		return tokenService.createToken();
	}
}
