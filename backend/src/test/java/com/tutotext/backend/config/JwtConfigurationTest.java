package com.tutotext.backend.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtConfigurationTest {

	private final JwtConfiguration configuration = new JwtConfiguration();

	@Test
	void refusesMissingJwtSecret() {
		assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey(""));
	}

	@Test
	void refusesWeakJwtSecret() {
		assertThrows(
				IllegalStateException.class,
				() -> configuration.jwtSecretKey("c2hvcnQ="));
	}
}
