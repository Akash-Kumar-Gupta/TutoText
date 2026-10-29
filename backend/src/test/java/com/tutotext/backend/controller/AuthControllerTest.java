package com.tutotext.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.tutotext.backend.repository.AppUserRepository;
import java.util.Base64;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	private static final String EMAIL = "account-test@example.com";
	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
	}

	@Test
	void registersAndLogsInWithoutReturningOrStoringPlaintextPassword() throws Exception {
		String registration = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn()
				.getResponse()
				.getContentAsString();

		org.junit.jupiter.api.Assertions.assertFalse(objectMapper.readTree(registration).path("accessToken").asText().isBlank());
		var user = userRepository.findByEmail(EMAIL).orElseThrow();
		org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, user.getPasswordHash());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL.toUpperCase(), PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void rejectsDuplicateRegistrationWithoutDisclosingStorageDetails() throws Exception {
		registerAccount();

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL.toUpperCase(), PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("An account with this email already exists"))
				.andExpect(jsonPath("$.exception").doesNotExist());
	}

	@Test
	void rejectsInvalidCredentialsWithGenericUnauthorizedResponse() throws Exception {
		registerAccount();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL, "different-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"))
				.andExpect(jsonPath("$.exception").doesNotExist());
	}

	@Test
	void rejectsMalformedAndExpiredJwtForProtectedApis() throws Exception {
		registerAccount();

		String signedToken = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder()
						.issuer("tutotext")
						.subject(EMAIL)
						.issuedAt(Instant.now())
						.expiresAt(Instant.now().plusSeconds(600))
						.build())).getTokenValue();
		String[] tokenParts = signedToken.split("\\.");
		byte[] signature = Base64.getUrlDecoder().decode(tokenParts[2]);
		signature[0] ^= 0x01;
		String invalidSignature = tokenParts[0] + "." + tokenParts[1] + "." +
				Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

		mockMvc.perform(get("/api/transcriptions")
				.header("Authorization", "Bearer " + invalidSignature))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication is required or the token is invalid"))
				.andExpect(jsonPath("$.exception").doesNotExist());

		String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder()
						.issuer("tutotext")
						.subject(EMAIL)
						.issuedAt(Instant.now().minusSeconds(7200))
						.expiresAt(Instant.now().minusSeconds(3600))
						.build())).getTokenValue();

		mockMvc.perform(get("/api/transcriptions")
				.header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication is required or the token is invalid"));
	}

	@Test
	void rejectsUnauthenticatedProtectedApiRequests() throws Exception {
		mockMvc.perform(get("/api/transcriptions"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Authentication is required or the token is invalid"));

		mockMvc.perform(post("/api/live-sessions/token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Authentication is required or the token is invalid"));

		mockMvc.perform(post("/api/transcriptions")
				.header("Content-Length", "17825793"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication is required or the token is invalid"));
	}

	@Test
	void validatesRegistrationFields() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson("not-an-email", "short")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.email").isNotEmpty())
				.andExpect(jsonPath("$.validationErrors.password").isNotEmpty());
	}

	@Test
	void rejectsUtf8PasswordsThatExceedBcryptByteLimit() throws Exception {
		String multibytePassword = "é".repeat(40);

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL, multibytePassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Password must be no more than 72 UTF-8 bytes"));
	}

	@Test
	void rejectsAuthenticationRequestBodiesAboveConfiguredLimit() throws Exception {
		String oversizedPassword = "a".repeat(17_000);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL, oversizedPassword)))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.status").value(413))
				.andExpect(jsonPath("$.message").value("Request body exceeds the allowed size"));
	}

	private void registerAccount() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(authJson(EMAIL, PASSWORD)))
				.andExpect(status().isCreated());
	}

	private String authJson(String email, String password) {
		return """
				{"email":"%s","password":"%s"}
				""".formatted(email, password);
	}
}
