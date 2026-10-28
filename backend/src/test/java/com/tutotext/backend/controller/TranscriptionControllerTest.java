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

	@BeforeEach
	void clearDatabase() {
		repository.deleteAll();
	}

	@Test
	void createListUpdateAndDeleteTranscription() throws Exception {
		mockMvc.perform(post("/api/transcriptions")
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
		String createdAt = repository.findById(id).orElseThrow().getCreatedAt().toString();
		mockMvc.perform(get("/api/transcriptions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].transcriptText").value("Hello from the meeting."));

		mockMvc.perform(put("/api/transcriptions/{id}", id)
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

		mockMvc.perform(get("/api/transcriptions/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transcriptText").value("Updated transcript."));

		mockMvc.perform(delete("/api/transcriptions/{id}", id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/transcriptions/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Transcription with id " + id + " was not found"));
	}

	@Test
	void rejectsInvalidCreateRequestWithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/transcriptions")
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
		mockMvc.perform(get("/api/transcriptions/0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request parameter validation failed"));
	}
}
