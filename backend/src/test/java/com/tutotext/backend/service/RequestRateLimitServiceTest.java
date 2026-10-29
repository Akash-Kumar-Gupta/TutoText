package com.tutotext.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tutotext.backend.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

class RequestRateLimitServiceTest {

	@Test
	void appliesIpAndAccountBudgetsAndAllowsDifferentClients() {
		RequestRateLimitService service = limiter(2, 2, 30);

		assertDoesNotThrow(() -> service.checkIpLimit("register", "192.0.2.1"));
		assertDoesNotThrow(() -> service.checkIpLimit("register", "192.0.2.1"));
		assertThrows(RateLimitExceededException.class,
				() -> service.checkIpLimit("register", "192.0.2.1"));
		assertDoesNotThrow(() -> service.checkIpLimit("register", "192.0.2.2"));

		service.checkRegistrationEmail("Person@example.com");
		service.checkRegistrationEmail("person@example.com");
		assertThrows(RateLimitExceededException.class,
				() -> service.checkRegistrationEmail("PERSON@example.com"));
	}

	@Test
	void refillsBudgetAfterConfiguredWindow() throws InterruptedException {
		RequestRateLimitService service = limiter(1, 1, 1);

		service.checkLoginEmail("person@example.com");
		assertThrows(RateLimitExceededException.class,
				() -> service.checkLoginEmail("person@example.com"));
		Thread.sleep(1_100);
		assertDoesNotThrow(() -> service.checkLoginEmail("person@example.com"));
	}

	@Test
	void changingAccountIdentityDoesNotBypassTheIpBudget() {
		RequestRateLimitService service = limiter(2, 10, 30);

		service.checkIpLimit("live-token", "192.0.2.1");
		service.checkIpLimit("live-token", "192.0.2.1");
		assertThrows(RateLimitExceededException.class,
				() -> service.checkIpLimit("live-token", "192.0.2.1"));
	}

	private RequestRateLimitService limiter(int capacity, int accountCapacity, long windowSeconds) {
		return new RequestRateLimitService(
				capacity,
				accountCapacity,
				windowSeconds,
				capacity,
				accountCapacity,
				windowSeconds,
				capacity,
				accountCapacity,
				windowSeconds,
				60);
	}
}
