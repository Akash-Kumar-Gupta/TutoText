package com.tutotext.backend.service;

import com.tutotext.backend.dto.GeminiEphemeralToken;
import com.tutotext.backend.dto.LiveSessionTokenResponse;
import com.tutotext.backend.exception.GeminiConfigurationException;
import com.tutotext.backend.exception.GeminiTokenProvisioningException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GeminiLiveTokenService {

	static final String MODEL = "gemini-3.5-transcribe-live";
	private static final String TOKEN_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/auth_tokens";

	private final RestClient restClient;
	private final String apiKey;

	public GeminiLiveTokenService(
			RestClient.Builder restClientBuilder,
			@Value("${gemini.api-key:}") String apiKey) {
		this.restClient = restClientBuilder.build();
		this.apiKey = apiKey;
	}

	public LiveSessionTokenResponse createToken() {
		if (apiKey.isBlank()) {
			throw new GeminiConfigurationException();
		}

		Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofMinutes(30));
		Map<String, Object> transcriptionConfig = Map.of("languageCodes", List.of());
		Map<String, Object> generationConfig = Map.of("responseModalities", List.of("TEXT"));
		Map<String, Object> liveSetup = Map.of(
				"model", "models/" + MODEL,
				"generationConfig", generationConfig,
				"inputAudioTranscription", transcriptionConfig);
		Map<String, Object> request = Map.of(
				"uses", 1,
				"expireTime", expiresAt.toString(),
				"newSessionExpireTime", now.plus(Duration.ofMinutes(1)).toString(),
				"bidiGenerateContentSetup", liveSetup);

		GeminiEphemeralToken response;
		try {
			response = restClient.post()
					.uri(TOKEN_ENDPOINT)
					.header("x-goog-api-key", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(GeminiEphemeralToken.class);
		} catch (RestClientException exception) {
			throw new GeminiTokenProvisioningException();
		}

		if (response == null
				|| response.name() == null
				|| response.name().isBlank()) {
			throw new GeminiTokenProvisioningException();
		}

		Instant tokenExpiry = response.expireTime() == null ? expiresAt : response.expireTime();
		return new LiveSessionTokenResponse(response.name(), tokenExpiry, MODEL);
	}
}
