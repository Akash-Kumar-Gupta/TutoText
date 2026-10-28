package com.tutotext.backend.exception;

public class GeminiConfigurationException extends RuntimeException {

	public GeminiConfigurationException() {
		super("Gemini Live API is not configured");
	}
}
