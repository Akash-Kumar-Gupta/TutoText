package com.tutotext.backend.dto;

import java.time.Instant;

public record GeminiEphemeralToken(String name, Instant expireTime) {
}
