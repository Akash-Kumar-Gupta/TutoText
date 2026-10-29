package com.tutotext.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tutotext.backend.service.RequestRateLimitService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

class RequestRateLimitFilterTest {

	@Test
	void returnsStructured429WithRetryAfterAfterIpBudgetIsExhausted() throws Exception {
		RequestRateLimitService service = new RequestRateLimitService(
				1, 1, 60,
				1, 1, 60,
				1, 1, 60,
				120);
		RequestRateLimitFilter filter = new RequestRateLimitFilter(service, new tools.jackson.databind.ObjectMapper());
		FilterChain chain = (request, response) -> {};

		MockHttpServletRequest allowedRequest = requestFrom("192.0.2.10");
		MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
		filter.doFilter(allowedRequest, allowedResponse, chain);
		assertEquals(200, allowedResponse.getStatus());

		MockHttpServletRequest limitedRequest = requestFrom("192.0.2.10");
		MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
		filter.doFilter(limitedRequest, limitedResponse, chain);

		assertEquals(429, limitedResponse.getStatus());
		assertTrue(Long.parseLong(limitedResponse.getHeader("Retry-After")) > 0);
		assertTrue(limitedResponse.getContentAsString().contains("Too many requests"));
	}

	@Test
	void appliesTranscriptBodyLimitOnlyAfterAuthentication() throws Exception {
		RequestBodyLimitFilter filter = new RequestBodyLimitFilter(
				new tools.jackson.databind.ObjectMapper(),
				16_384,
				16 * 1024 * 1024,
				65_536);
		int oversizedLength = 16 * 1024 * 1024 + 1;
		MockHttpServletRequest unauthenticated = requestWithLength("/api/transcriptions", oversizedLength);
		MockHttpServletResponse unauthenticatedResponse = new MockHttpServletResponse();
		filter.doFilter(unauthenticated, unauthenticatedResponse, (request, response) ->
				assertEquals(200, ((MockHttpServletResponse) response).getStatus()));
		assertEquals(200, unauthenticatedResponse.getStatus());

		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));
		try {
			MockHttpServletRequest authenticated = requestWithLength("/api/transcriptions", oversizedLength);
			MockHttpServletResponse authenticatedResponse = new MockHttpServletResponse();
			filter.doFilter(authenticated, authenticatedResponse, (request, response) -> {});
			assertEquals(413, authenticatedResponse.getStatus());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private MockHttpServletRequest requestFrom(String remoteAddress) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
		request.setRemoteAddr(remoteAddress);
		return request;
	}

	private MockHttpServletRequest requestWithLength(String uri, int length) {
		return new MockHttpServletRequest("POST", uri) {
			@Override
			public int getContentLength() {
				return length;
			}

			@Override
			public long getContentLengthLong() {
				return length;
			}
		};
	}
}
