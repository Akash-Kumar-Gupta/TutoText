package com.tutotext.backend.dto;

import java.time.Instant;

public record LiveSessionTokenResponse(String token, Instant expiresAt, String model) {
}
