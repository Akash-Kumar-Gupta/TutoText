package com.tutotext.backend.config;

import com.tutotext.backend.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

@Component
public class RequestBodyLimitFilter extends OncePerRequestFilter {

	private static final int MIB = 1024 * 1024;
	private final ObjectMapper objectMapper;
	private final int authLimitBytes;
	private final int transcriptionLimitBytes;
	private final int otherApiLimitBytes;

	public RequestBodyLimitFilter(
			ObjectMapper objectMapper,
			@Value("${app.request-size.auth-bytes:16384}") int authLimitBytes,
			@Value("${app.request-size.transcription-bytes:17825792}") int transcriptionLimitBytes,
			@Value("${app.request-size.other-api-bytes:65536}") int otherApiLimitBytes) {
		this.objectMapper = objectMapper;
		this.authLimitBytes = positive(authLimitBytes);
		this.transcriptionLimitBytes = positive(transcriptionLimitBytes);
		this.otherApiLimitBytes = positive(otherApiLimitBytes);
		if (this.transcriptionLimitBytes < 16 * MIB) {
			throw new IllegalStateException("Transcription request limit must allow a 4,000,000-character UTF-8 transcript");
		}
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (!isPublicAuthenticationRequest(request) && !isAuthenticated()) {
			filterChain.doFilter(request, response);
			return;
		}
		int limit = requestLimit(request);
		if (limit == 0 || !hasRequestBody(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (request.getContentLengthLong() > limit) {
			writeTooLarge(request, response);
			return;
		}
		byte[] body = request.getInputStream().readNBytes(limit + 1);
		if (body.length > limit) {
			writeTooLarge(request, response);
			return;
		}
		filterChain.doFilter(new CachedBodyRequest(request, body), response);
	}

	private boolean isPublicAuthenticationRequest(HttpServletRequest request) {
		return "POST".equalsIgnoreCase(request.getMethod())
				&& ("/api/auth/login".equals(request.getRequestURI())
						|| "/api/auth/register".equals(request.getRequestURI()));
	}

	private boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	private int requestLimit(HttpServletRequest request) {
		if (!request.getRequestURI().startsWith("/api/")) return 0;
		if (isPublicAuthenticationRequest(request)) return authLimitBytes;
		if (request.getRequestURI().equals("/api/transcriptions")
				|| request.getRequestURI().startsWith("/api/transcriptions/")) {
			return transcriptionLimitBytes;
		}
		return otherApiLimitBytes;
	}

	private boolean hasRequestBody(HttpServletRequest request) {
		String method = request.getMethod();
		return "POST".equalsIgnoreCase(method)
				|| "PUT".equalsIgnoreCase(method)
				|| "PATCH".equalsIgnoreCase(method);
	}

	private void writeTooLarge(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
				Instant.now(),
				HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
				"Payload Too Large",
				"Request body exceeds the allowed size",
				request.getRequestURI(),
				Map.of()));
	}

	private static int positive(int value) {
		if (value < 1) throw new IllegalStateException("Request body limits must be positive");
		return value;
	}

	private static final class CachedBodyRequest extends HttpServletRequestWrapper {
		private final byte[] body;

		private CachedBodyRequest(HttpServletRequest request, byte[] body) {
			super(request);
			this.body = body;
		}

		@Override
		public ServletInputStream getInputStream() {
			ByteArrayInputStream input = new ByteArrayInputStream(body);
			return new ServletInputStream() {
				@Override
				public int read() {
					return input.read();
				}

				@Override
				public boolean isFinished() {
					return input.available() == 0;
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setReadListener(ReadListener readListener) {
					try {
						if (input.available() > 0) readListener.onDataAvailable();
						if (input.available() == 0) readListener.onAllDataRead();
					} catch (IOException exception) {
						readListener.onError(exception);
					}
				}
			};
		}

		@Override
		public BufferedReader getReader() throws IOException {
			String encoding = getCharacterEncoding();
			Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
			return new BufferedReader(new InputStreamReader(getInputStream(), charset));
		}
	}
}
