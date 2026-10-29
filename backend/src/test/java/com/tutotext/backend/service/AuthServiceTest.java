package com.tutotext.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutotext.backend.exception.InvalidCredentialsException;
import com.tutotext.backend.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class AuthServiceTest {

	@Test
	void performsPasswordHashVerificationForUnknownAccounts() {
		AppUserRepository userRepository = org.mockito.Mockito.mock(AppUserRepository.class);
		PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
		JwtEncoder jwtEncoder = org.mockito.Mockito.mock(JwtEncoder.class);
		when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("dummy-hash");
		when(userRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());
		AuthService service = new AuthService(userRepository, passwordEncoder, jwtEncoder, 900_000);

		assertThrows(InvalidCredentialsException.class,
				() -> service.login(new com.tutotext.backend.dto.AuthRequest(
						"missing@example.com", "valid-test-password")));
		verify(passwordEncoder).matches("valid-test-password", "dummy-hash");
	}
}
