package com.tutotext.backend.service;

import com.tutotext.backend.dto.AuthRequest;
import com.tutotext.backend.dto.AuthResponse;
import com.tutotext.backend.entity.AppUser;
import com.tutotext.backend.exception.EmailAlreadyRegisteredException;
import com.tutotext.backend.exception.InvalidCredentialsException;
import com.tutotext.backend.exception.PasswordTooLongException;
import com.tutotext.backend.repository.AppUserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
	private static final String ISSUER = "tutotext";

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final Duration tokenLifetime;
	private final String dummyPasswordHash;

	public AuthService(
			AppUserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtEncoder jwtEncoder,
			@Value("${app.security.jwt.expiration-ms:900000}") long tokenLifetimeMs) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
		if (tokenLifetimeMs < 60_000 || tokenLifetimeMs > 86_400_000) {
			throw new IllegalStateException("JWT lifetime must be between one minute and one day");
		}
		this.tokenLifetime = Duration.ofMillis(tokenLifetimeMs);
	}

	public AuthResponse register(AuthRequest request) {
		validateBcryptLength(request.password());
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyRegisteredException();
		}

		AppUser user = new AppUser(email, passwordEncoder.encode(request.password()));
		try {
			user = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			throw new EmailAlreadyRegisteredException();
		}
		return createToken(user);
	}

	public AuthResponse login(AuthRequest request) {
		if (!hasValidBcryptLength(request.password())) {
			throw new InvalidCredentialsException();
		}
		String email = normalizeEmail(request.email());
		AppUser user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			throw new InvalidCredentialsException();
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return createToken(user);
	}

	private AuthResponse createToken(AppUser user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(tokenLifetime);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getEmail())
				.claim("userId", user.getId())
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();
		return new AuthResponse(token, "Bearer", expiresAt, user.getEmail());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private void validateBcryptLength(String password) {
		if (!hasValidBcryptLength(password)) {
			throw new PasswordTooLongException();
		}
	}

	private boolean hasValidBcryptLength(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_PASSWORD_BYTES;
	}
}
