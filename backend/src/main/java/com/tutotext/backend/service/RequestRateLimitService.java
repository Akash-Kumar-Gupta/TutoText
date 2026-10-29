package com.tutotext.backend.service;

import com.tutotext.backend.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RequestRateLimitService {

	private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
	private final int registrationIpCapacity;
	private final int registrationEmailCapacity;
	private final Duration registrationWindow;
	private final int loginIpCapacity;
	private final int loginEmailCapacity;
	private final Duration loginWindow;
	private final int liveTokenIpCapacity;
	private final int liveTokenAccountCapacity;
	private final Duration liveTokenWindow;
	private final Duration bucketRetention;

	public RequestRateLimitService(
			@Value("${app.rate-limit.registration.ip-capacity:5}") int registrationIpCapacity,
			@Value("${app.rate-limit.registration.email-capacity:3}") int registrationEmailCapacity,
			@Value("${app.rate-limit.registration.window-seconds:3600}") long registrationWindowSeconds,
			@Value("${app.rate-limit.login.ip-capacity:20}") int loginIpCapacity,
			@Value("${app.rate-limit.login.email-capacity:10}") int loginEmailCapacity,
			@Value("${app.rate-limit.login.window-seconds:900}") long loginWindowSeconds,
			@Value("${app.rate-limit.live-token.ip-capacity:10}") int liveTokenIpCapacity,
			@Value("${app.rate-limit.live-token.account-capacity:5}") int liveTokenAccountCapacity,
			@Value("${app.rate-limit.live-token.window-seconds:600}") long liveTokenWindowSeconds,
			@Value("${app.rate-limit.bucket-retention-seconds:7200}") long bucketRetentionSeconds) {
		this.registrationIpCapacity = positive(registrationIpCapacity, "registration IP capacity");
		this.registrationEmailCapacity = positive(registrationEmailCapacity, "registration email capacity");
		this.registrationWindow = positiveDuration(registrationWindowSeconds, "registration window");
		this.loginIpCapacity = positive(loginIpCapacity, "login IP capacity");
		this.loginEmailCapacity = positive(loginEmailCapacity, "login email capacity");
		this.loginWindow = positiveDuration(loginWindowSeconds, "login window");
		this.liveTokenIpCapacity = positive(liveTokenIpCapacity, "live token IP capacity");
		this.liveTokenAccountCapacity = positive(liveTokenAccountCapacity, "live token account capacity");
		this.liveTokenWindow = positiveDuration(liveTokenWindowSeconds, "live token window");
		Duration configuredRetention = positiveDuration(bucketRetentionSeconds, "bucket retention");
		this.bucketRetention = List.of(
						configuredRetention,
						registrationWindow,
						loginWindow,
						liveTokenWindow)
				.stream()
				.max(Duration::compareTo)
				.orElseThrow();
	}

	public synchronized void checkIpLimit(String operation, String remoteAddress) {
		switch (operation) {
			case "register" -> consume(List.of(
					bucketKey("register-ip", remoteAddress)), registrationIpCapacity, registrationWindow);
			case "login" -> consume(List.of(
					bucketKey("login-ip", remoteAddress)), loginIpCapacity, loginWindow);
			case "live-token" -> consume(List.of(
					bucketKey("live-token-ip", remoteAddress)), liveTokenIpCapacity, liveTokenWindow);
			default -> throw new IllegalArgumentException("Unknown rate limit operation");
		}
	}

	public synchronized void checkRegistrationEmail(String email) {
		consume(List.of(bucketKey("register-email", email)),
				registrationEmailCapacity,
				registrationWindow);
	}

	public synchronized void checkLoginEmail(String email) {
		consume(List.of(bucketKey("login-email", email)),
				loginEmailCapacity,
				loginWindow);
	}

	public synchronized void checkLiveTokenAccount(String account) {
		consume(List.of(bucketKey("live-token-account", account)),
				liveTokenAccountCapacity,
				liveTokenWindow);
	}

	@Scheduled(fixedDelayString = "${app.rate-limit.cleanup-interval-ms:60000}")
	public synchronized void removeExpiredBuckets() {
		long inactiveBefore = System.nanoTime() - bucketRetention.toNanos();
		buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessNanos < inactiveBefore);
	}

	private void consume(List<String> keys, int capacity, Duration window) {
		List<BucketEntry> entries = new ArrayList<>(keys.size());
		for (String key : keys) {
			BucketEntry entry = buckets.computeIfAbsent(key, ignored -> createBucket(capacity, window));
			entry.lastAccessNanos = System.nanoTime();
			entries.add(entry);
		}

		for (BucketEntry entry : entries) {
			if (entry.bucket.getAvailableTokens() < 1) {
				long retryAfterSeconds = entry.bucket.tryConsumeAndReturnRemaining(1)
						.getNanosToWaitForRefill() / 1_000_000_000L;
				throw new RateLimitExceededException(retryAfterSeconds + 1);
			}
		}
		entries.forEach(entry -> entry.bucket.tryConsume(1));
	}

	private BucketEntry createBucket(int capacity, Duration window) {
		Bandwidth limit = Bandwidth.builder()
				.capacity(capacity)
				.refillGreedy(capacity, window)
				.build();
		return new BucketEntry(Bucket.builder().addLimit(limit).build());
	}

	private String normalizeAndHash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(value.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private String bucketKey(String operation, String discriminator) {
		return operation + ":" + normalizeAndHash(discriminator);
	}

	private static int positive(int value, String name) {
		if (value < 1) throw new IllegalStateException(name + " must be positive");
		return value;
	}

	private static Duration positiveDuration(long seconds, String name) {
		if (seconds < 1) throw new IllegalStateException(name + " must be positive");
		return Duration.ofSeconds(seconds);
	}

	private static final class BucketEntry {
		private final Bucket bucket;
		private volatile long lastAccessNanos = System.nanoTime();

		private BucketEntry(Bucket bucket) {
			this.bucket = bucket;
		}
	}
}
