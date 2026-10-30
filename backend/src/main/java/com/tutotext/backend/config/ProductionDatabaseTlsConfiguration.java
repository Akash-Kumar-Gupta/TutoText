package com.tutotext.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("production")
public class ProductionDatabaseTlsConfiguration {

	public ProductionDatabaseTlsConfiguration(
			@Value("${spring.datasource.hikari.data-source-properties.sslMode:}") String sslMode) {
		if (!"VERIFY_IDENTITY".equals(sslMode)) {
			throw new IllegalStateException("Production database SSL mode must be VERIFY_IDENTITY");
		}
	}
}
