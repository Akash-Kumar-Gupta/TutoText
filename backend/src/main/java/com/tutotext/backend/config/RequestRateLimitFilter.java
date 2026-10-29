package com.tutotext.backend.config;

import com.tutotext.backend.dto.ApiErrorResponse;
import com.tutotext.backend.exception.RateLimitExceededException;
import com.tutotext.backend.service.RequestRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestRateLimitFilter extends OncePerRequestFilter {

	private final RequestRateLimitService rateLimitService;
	private final ObjectMapper objectMapper;

	public RequestRateLimitFilter(RequestRateLimitService rateLimitService, ObjectMapper objectMapper) {
		this.rateLimitService = rateLimitService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String operation = operationFor(request);
		if (operation == null) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			rateLimitService.checkIpLimit(operation, request.getRemoteAddr());
			filterChain.doFilter(request, response);
		} catch (RateLimitExceededException exception) {
			response.setHeader("Retry-After", Long.toString(exception.getRetryAfterSeconds()));
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
					Instant.now(),
					HttpStatus.TOO_MANY_REQUESTS.value(),
					"Too Many Requests",
					"Too many requests. Please try again later.",
					request.getRequestURI(),
					Map.of()));
		}
	}

	private String operationFor(HttpServletRequest request) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) return null;
		return switch (request.getRequestURI()) {
			case "/api/auth/register" -> "register";
			case "/api/auth/login" -> "login";
			case "/api/live-sessions/token" -> "live-token";
			default -> null;
		};
	}
}
