package com.tutotext.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tutotext.backend.dto.LiveSessionTokenResponse;
import com.tutotext.backend.exception.GeminiConfigurationException;
import com.tutotext.backend.exception.GeminiTokenProvisioningException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiLiveTokenServiceTest {

	private static final String TOKEN_ENDPOINT =
			"https://generativelanguage.googleapis.com/v1beta/auth_tokens";

	@Test
	void createsSingleUseTokenConstrainedToLiveTranscription() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(TOKEN_ENDPOINT))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", "test-key-not-a-real-credential"))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "uses": 1,
						  "bidiGenerateContentSetup": {
						    "model": "models/gemini-3.5-transcribe-live",
						    "generationConfig": {"responseModalities": ["TEXT"]},
						    "inputAudioTranscription": {"languageCodes": []}
						  }
						}
						""", false))
				.andExpect(jsonPath("$.bidiGenerateContentSetup.generationConfig.responseModalities[0]")
						.value("TEXT"))
				.andExpect(jsonPath("$.liveConnectConstraints").doesNotExist())
				.andRespond(withSuccess("""
						{
						  "name": "authTokens/test-ephemeral-token",
						  "expireTime": "2099-10-27T04:00:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		GeminiLiveTokenService service = new GeminiLiveTokenService(builder, "test-key-not-a-real-credential");
		LiveSessionTokenResponse response = service.createToken();

		assertEquals("authTokens/test-ephemeral-token", response.token());
		assertEquals(Instant.parse("2099-10-27T04:00:00Z"), response.expiresAt());
		assertEquals(GeminiLiveTokenService.MODEL, response.model());
		server.verify();
	}

	@Test
	void doesNotCallProviderWhenApiKeyIsNotConfigured() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GeminiLiveTokenService service = new GeminiLiveTokenService(builder, " ");

		assertThrows(GeminiConfigurationException.class, service::createToken);
		server.verify();
	}

	@Test
	void returnsSanitizedFailureWhenProviderRejectsTheRequest() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(TOKEN_ENDPOINT))
				.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
						.withServerError()
						.body("sensitive provider response"));
		GeminiLiveTokenService service = new GeminiLiveTokenService(builder, "test-key");

		GeminiTokenProvisioningException exception = assertThrows(
				GeminiTokenProvisioningException.class,
				service::createToken);

		assertEquals("Gemini Live API could not provision a session token", exception.getMessage());
		server.verify();
	}

	@Test
	void usesRequestedExpiryWhenProviderOmitsExpiry() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(TOKEN_ENDPOINT))
				.andRespond(withSuccess("""
						{"name": "authTokens/test-ephemeral-token"}
						""", MediaType.APPLICATION_JSON));
		GeminiLiveTokenService service = new GeminiLiveTokenService(builder, "test-key");

		Instant beforeRequest = Instant.now();
		LiveSessionTokenResponse response = service.createToken();
		Instant afterRequest = Instant.now();

		assertEquals("authTokens/test-ephemeral-token", response.token());
		assertEquals(GeminiLiveTokenService.MODEL, response.model());
		assertTrue(response.expiresAt().isAfter(beforeRequest.plusSeconds(29 * 60L)));
		assertTrue(response.expiresAt().isBefore(afterRequest.plusSeconds(31 * 60L)));
		server.verify();
	}

	@Test
	void rejectsProviderResponseWithMissingName() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(TOKEN_ENDPOINT))
				.andRespond(withSuccess("""
						{"expireTime": "2099-10-27T04:00:00Z"}
						""", MediaType.APPLICATION_JSON));
		GeminiLiveTokenService service = new GeminiLiveTokenService(builder, "test-key");

		assertThrows(GeminiTokenProvisioningException.class, service::createToken);
		server.verify();
	}

	@Test
	void rejectsProviderResponseWithBlankName() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(TOKEN_ENDPOINT))
				.andRespond(withSuccess("""
						{"name": "  ", "expireTime": "2099-10-27T04:00:00Z"}
						""", MediaType.APPLICATION_JSON));
		GeminiLiveTokenService service = new GeminiLiveTokenService(builder, "test-key");

		assertThrows(GeminiTokenProvisioningException.class, service::createToken);
		server.verify();
	}
}
