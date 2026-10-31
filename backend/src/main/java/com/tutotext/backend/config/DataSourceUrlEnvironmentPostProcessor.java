package com.tutotext.backend.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.MapPropertySource;

public class DataSourceUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final String PROPERTY_SOURCE_NAME = "tutotextDataSourceUrl";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		boolean production = environment.acceptsProfiles(Profiles.of("production"));
		String host = environment.getProperty("DB_HOST");
		String port = environment.getProperty("DB_PORT");
		String database = environment.getProperty("DB_NAME");
		if (production && (isBlank(host) || isBlank(port) || isBlank(database))) {
			throw new IllegalStateException(
					"Production database coordinates DB_HOST, DB_PORT, and DB_NAME must be configured");
		}
		Map<String, Object> mappedProperties = new HashMap<>();
		mapIfPresent(environment, mappedProperties, "DB_USERNAME", "SPRING_DATASOURCE_USERNAME");
		mapIfPresent(environment, mappedProperties, "DB_PASSWORD", "SPRING_DATASOURCE_PASSWORD");
		mapIfPresent(environment, mappedProperties, "JWT_SECRET", "APP_SECURITY_JWT_SECRET");
		if (hasExplicitDatasourceUrl(environment)) {
			if (production && isLocalDatabaseUrl(environment.getProperty("spring.datasource.url", ""))) {
				throw new IllegalStateException("Production database URL must not use a local host");
			}
			addMappedProperties(environment, mappedProperties);
			return;
		}

		String databaseUrl = environment.getProperty("DB_URL");
		if (isBlank(databaseUrl)) {
			databaseUrl = "jdbc:mysql://" + defaultIfBlank(host, "localhost")
					+ ":" + defaultIfBlank(port, "3306")
					+ "/" + defaultIfBlank(database, "tutotext_db");
		}
		if (production && isLocalDatabaseUrl(databaseUrl)) {
			throw new IllegalStateException("Production database URL must not use a local host");
		}
		mappedProperties.put("SPRING_DATASOURCE_URL", databaseUrl);
		addMappedProperties(environment, mappedProperties);
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private boolean hasExplicitDatasourceUrl(ConfigurableEnvironment environment) {
		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			Object value = propertySource.getProperty("spring.datasource.url");
			if (value instanceof String url && !url.equals("${SPRING_DATASOURCE_URL}")) {
				return true;
			}
		}
		return false;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String defaultIfBlank(String value, String defaultValue) {
		return isBlank(value) ? defaultValue : value;
	}

	private void mapIfPresent(
			ConfigurableEnvironment environment,
			Map<String, Object> mappedProperties,
			String sourceName,
			String targetName) {
		String value = environment.getProperty(sourceName);
		if (value != null) {
			mappedProperties.put(targetName, value);
		}
	}

	private void addMappedProperties(ConfigurableEnvironment environment, Map<String, Object> mappedProperties) {
		if (!mappedProperties.isEmpty()) {
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, mappedProperties));
		}
	}

	private boolean isLocalDatabaseUrl(String databaseUrl) {
		String prefix = "jdbc:mysql://";
		if (!databaseUrl.regionMatches(true, 0, prefix, 0, prefix.length())) {
			return false;
		}
		String hostAndPort = databaseUrl.substring(prefix.length()).split("[/?#]", 2)[0];
		String host = hostAndPort.startsWith("[")
				? hostAndPort.substring(1, hostAndPort.indexOf(']'))
				: hostAndPort.split(":", 2)[0];
		return host.equalsIgnoreCase("localhost")
				|| host.equals("127.0.0.1")
				|| host.equals("::1");
	}
}
