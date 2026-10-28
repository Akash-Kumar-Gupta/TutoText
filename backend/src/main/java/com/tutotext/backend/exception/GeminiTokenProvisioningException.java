package com.tutotext.backend.exception;

public class GeminiTokenProvisioningException extends RuntimeException {

	public GeminiTokenProvisioningException() {
		super("Gemini Live API could not provision a session token");
	}
}
