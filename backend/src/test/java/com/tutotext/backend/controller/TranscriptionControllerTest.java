package com.tutotext.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutotext.backend.repository.TranscriptionRepository;
import com.tutotext.backend.repository.AppUserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TranscriptionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TranscriptionRepository repository;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private String accessToken;

	@BeforeEach
	void clearDatabase() throws Exception {
		repository.deleteAll();
		userRepository.deleteAll();
		String response = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"transcription-test@example.com","password":"long-test-password"}
						"""))
				.andReturn()
				.getResponse()
				.getContentAsString();
		accessToken = objectMapper.readTree(response).path("accessToken").asText();
	}

	@Test
	void createListUpdateAndDeleteTranscription() throws Exception {
		mockMvc.perform(post("/api/transcriptions")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Team meeting",
						  "transcriptText": "Hello from the meeting.",
						  "language": "English",
						  "durationSeconds": 90
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("Team meeting"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		Long id = repository.findAll().getFirst().getId();
		org.junit.jupiter.api.Assertions.assertEquals(
				1,
				repository.findAllByOwner_Id(
						userRepository.findByEmail("transcription-test@example.com").orElseThrow().getId(),
						org.springframework.data.domain.Sort.unsorted()).size());
		String createdAt = repository.findById(id).orElseThrow().getCreatedAt().toString();
		mockMvc.perform(get("/api/transcriptions")
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].transcriptText").value("Hello from the meeting."));

		mockMvc.perform(put("/api/transcriptions/{id}", id)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Updated meeting",
						  "transcriptText": "Updated transcript.",
						  "language": "Hindi",
						  "durationSeconds": 120
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated meeting"))
				.andExpect(jsonPath("$.language").value("Hindi"))
				.andExpect(jsonPath("$.durationSeconds").value(120))
				.andExpect(jsonPath("$.updatedAt").value(not(createdAt)));

		mockMvc.perform(get("/api/transcriptions/{id}", id)
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transcriptText").value("Updated transcript."));

		mockMvc.perform(delete("/api/transcriptions/{id}", id)
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/transcriptions/{id}", id)
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Transcription with id " + id + " was not found"));
	}

	@Test
	void isolatesTranscriptOperationsBetweenTwoAuthenticatedUsers() throws Exception {
		String userBToken = registerAndGetToken("other-user@example.com");
		mockMvc.perform(post("/api/transcriptions")
				.header("Authorization", "Bearer " + userBToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(transcriptionJson("User B transcript", "Private user B text")))
				.andExpect(status().isCreated());

		Long userBTranscriptId = repository.findAll().getFirst().getId();
		mockMvc.perform(post("/api/transcriptions")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(transcriptionJson("User A transcript", "Private user A text")))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/transcriptions")
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("User A transcript"));

		mockMvc.perform(get("/api/transcriptions/{id}", userBTranscriptId)
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(put("/api/transcriptions/{id}", userBTranscriptId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(transcriptionJson("Hijacked title", "Hijacked text")))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/transcriptions/{id}", userBTranscriptId)
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/transcriptions/{id}", userBTranscriptId)
				.header("Authorization", "Bearer " + userBToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("User B transcript"))
				.andExpect(jsonPath("$.transcriptText").value("Private user B text"));
	}

	@Test
	void rejectsInvalidCreateRequestWithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/transcriptions")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": " ",
						  "transcriptText": "",
						  "language": "English",
						  "durationSeconds": -1
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.validationErrors.title").isNotEmpty())
				.andExpect(jsonPath("$.validationErrors.transcriptText").isNotEmpty())
				.andExpect(jsonPath("$.validationErrors.durationSeconds").isNotEmpty());
	}

	@Test
	void rejectsNonPositiveId() throws Exception {
		mockMvc.perform(get("/api/transcriptions/0")
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request parameter validation failed"));
	}

	@Test
	void acceptsTranscriptAtTheExistingCharacterLimit() throws Exception {
		String transcript = "a".repeat(4_000_000);
		String request = objectMapper.writeValueAsString(new com.tutotext.backend.dto.TranscriptionRequest(
				"Long transcript", transcript, "English", 1));

		mockMvc.perform(post("/api/transcriptions")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isCreated());

		org.junit.jupiter.api.Assertions.assertEquals(
				4_000_000,
				repository.findAllByOwner_Id(
						userRepository.findByEmail("transcription-test@example.com").orElseThrow().getId(),
						org.springframework.data.domain.Sort.unsorted())
						.getFirst()
						.getTranscriptText()
						.length());
	}

	private String registerAndGetToken(String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"long-test-password"}
						""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).path("accessToken").asText();
	}

	private String transcriptionJson(String title, String transcriptText) {
		return """
				{
				  "title": "%s",
				  "transcriptText": "%s",
				  "language": "English",
				  "durationSeconds": 90
				}
				""".formatted(title, transcriptText);
	}
}
