package com.tutotext.backend.config;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void readinessIsPublicAndDoesNotExposeHealthDetails() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")))
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist());
	}

	@Test
	void otherActuatorAndApplicationEndpointsRemainProtected() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/transcriptions"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedRequestsCanAccessHealthAndApplicationApis() throws Exception {
		String response = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"health-check-test@example.com","password":"long-test-password"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String accessToken = objectMapper.readTree(response).path("accessToken").asText();

		mockMvc.perform(get("/actuator/health")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")));
		mockMvc.perform(get("/api/transcriptions")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
		for (String endpoint : new String[] {
				"/actuator/env",
				"/actuator/configprops",
				"/actuator/beans",
				"/actuator/mappings"
		}) {
			mockMvc.perform(get(endpoint)
							.header("Authorization", "Bearer " + accessToken))
					.andExpect(status().isNotFound());
		}
	}
}
