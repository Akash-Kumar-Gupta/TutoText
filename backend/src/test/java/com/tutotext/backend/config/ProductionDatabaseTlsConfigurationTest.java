package com.tutotext.backend.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionDatabaseTlsConfigurationTest {

	@Test
	void acceptsVerifiedIdentityMode() {
		assertDoesNotThrow(() -> new ProductionDatabaseTlsConfiguration("VERIFY_IDENTITY"));
	}

	@Test
	void rejectsMissingOrWeakerSslModes() {
		for (String sslMode : new String[] {"", "REQUIRED", "PREFERRED", "DISABLED"}) {
			assertThrows(
					IllegalStateException.class,
					() -> new ProductionDatabaseTlsConfiguration(sslMode));
		}
	}
}
