package com.tutotext.backend.controller;

import com.tutotext.backend.dto.LiveSessionTokenResponse;
import com.tutotext.backend.service.GeminiLiveTokenService;
import com.tutotext.backend.service.RequestRateLimitService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live-sessions")
public class LiveSessionController {

	private final GeminiLiveTokenService tokenService;
	private final RequestRateLimitService rateLimitService;

	public LiveSessionController(GeminiLiveTokenService tokenService, RequestRateLimitService rateLimitService) {
		this.tokenService = tokenService;
		this.rateLimitService = rateLimitService;
	}

	@PostMapping("/token")
	public LiveSessionTokenResponse createToken(Authentication authentication) {
		rateLimitService.checkLiveTokenAccount(authentication.getName());
		return tokenService.createToken();
	}
}
