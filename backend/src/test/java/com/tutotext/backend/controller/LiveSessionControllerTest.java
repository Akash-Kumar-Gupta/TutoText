package com.tutotext.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutotext.backend.dto.LiveSessionTokenResponse;
import com.tutotext.backend.exception.GeminiConfigurationException;
import com.tutotext.backend.exception.GlobalExceptionHandler;
import com.tutotext.backend.service.GeminiLiveTokenService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LiveSessionControllerTest {

	private GeminiLiveTokenService tokenService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		tokenService = org.mockito.Mockito.mock(GeminiLiveTokenService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new LiveSessionController(tokenService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createsTokenThroughTheLiveSessionEndpoint() throws Exception {
		when(tokenService.createToken()).thenReturn(new LiveSessionTokenResponse(
				"authTokens/test-token",
				Instant.parse("2026-10-27T04:00:00Z"),
				"gemini-3.5-transcribe-live"));

		mockMvc.perform(post("/api/live-sessions/token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("authTokens/test-token"))
				.andExpect(jsonPath("$.model").value("gemini-3.5-transcribe-live"));

		verify(tokenService).createToken();
	}

	@Test
	void reportsMissingGeminiConfigurationWithoutExposingInternalDetails() throws Exception {
		when(tokenService.createToken()).thenThrow(new GeminiConfigurationException());

		mockMvc.perform(post("/api/live-sessions/token"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("Live transcription is not configured on the server"))
				.andExpect(jsonPath("$.path").value("/api/live-sessions/token"));
	}
}
