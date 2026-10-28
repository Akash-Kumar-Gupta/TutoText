package com.tutotext.backend.exception;

import com.tutotext.backend.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(TranscriptionNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(
			TranscriptionNotFoundException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> validationErrors = new TreeMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, validationErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidPathParameter(
			HandlerMethodValidationException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request parameter validation failed", request, Map.of());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		Map<String, String> validationErrors = new TreeMap<>();
		exception.getConstraintViolations().forEach(violation -> {
			String propertyPath = violation.getPropertyPath().toString();
			String field = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
			validationErrors.putIfAbsent(field, violation.getMessage());
		});
		return error(HttpStatus.BAD_REQUEST, "Request parameter validation failed", request, validationErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", request, Map.of());
	}

	@ExceptionHandler(GeminiConfigurationException.class)
	public ResponseEntity<ApiErrorResponse> handleGeminiConfiguration(
			GeminiConfigurationException exception,
			HttpServletRequest request) {
		return error(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Live transcription is not configured on the server",
				request,
				Map.of());
	}

	@ExceptionHandler(GeminiTokenProvisioningException.class)
	public ResponseEntity<ApiErrorResponse> handleGeminiTokenProvisioning(
			GeminiTokenProvisioningException exception,
			HttpServletRequest request) {
		return error(
				HttpStatus.BAD_GATEWAY,
				"Could not start a Live API session. Check the Gemini API key, model access, and usage limits.",
				request,
				Map.of());
	}

	private ResponseEntity<ApiErrorResponse> error(
			HttpStatus status,
			String message,
			HttpServletRequest request,
			Map<String, String> validationErrors) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				validationErrors);
		return ResponseEntity.status(status).body(body);
	}
}
