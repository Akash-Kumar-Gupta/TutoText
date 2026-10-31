package com.tutotext.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

class DataSourceUrlEnvironmentPostProcessorTest {

	private final DataSourceUrlEnvironmentPostProcessor postProcessor =
			new DataSourceUrlEnvironmentPostProcessor();

	@Test
	void isRegisteredUsingSpringBootFourFactoryKey() {
		assertTrue(SpringFactoriesLoader.loadFactoryNames(
						EnvironmentPostProcessor.class,
						getClass().getClassLoader())
				.contains(DataSourceUrlEnvironmentPostProcessor.class.getName()));
	}

	@Test
	void databaseUrlOverrideBecomesNativeSpringDatasourceUrl() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_URL", "jdbc:mysql://override.example:3310/override_db");

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertEquals(
				"jdbc:mysql://override.example:3310/override_db",
				environment.getProperty("SPRING_DATASOURCE_URL"));
	}

	@Test
	void explicitNativeSpringDatasourceUrlTakesPrecedence() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_URL", "jdbc:mysql://compat.example:3309/compat_db");
		environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
				"nativeEnvironment",
				java.util.Map.of("SPRING_DATASOURCE_URL", "jdbc:mysql://native.example:3308/native_db")));

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertEquals(
				"jdbc:mysql://native.example:3308/native_db",
				environment.getProperty("spring.datasource.url"));
	}

	@Test
	void databaseCoordinatesComposeNativeSpringDatasourceUrl() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_HOST", "mysql.example")
				.withProperty("DB_PORT", "3307")
				.withProperty("DB_NAME", "tutotext");

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertEquals(
				"jdbc:mysql://mysql.example:3307/tutotext",
				environment.getProperty("SPRING_DATASOURCE_URL"));
	}

	@Test
	void legacyCredentialsMapToNativeSpringProperties() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_USERNAME", "local-user")
				.withProperty("DB_PASSWORD", "local-password")
				.withProperty("JWT_SECRET", "local-jwt-secret");

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("local-user", environment.getProperty("SPRING_DATASOURCE_USERNAME"));
		assertEquals("local-password", environment.getProperty("SPRING_DATASOURCE_PASSWORD"));
		assertEquals("local-jwt-secret", environment.getProperty("APP_SECURITY_JWT_SECRET"));
	}

	@Test
	void localConfigurationRetainsItsDefaults() {
		MockEnvironment environment = new MockEnvironment();

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertEquals(
				"jdbc:mysql://localhost:3306/tutotext_db",
				environment.getProperty("SPRING_DATASOURCE_URL"));
	}

	@Test
	void productionRequiresExplicitDatabaseCoordinatesEvenWithUrlOverride() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_URL", "jdbc:mysql://remote.example/db");
		environment.setActiveProfiles("production");

		assertThrows(
				IllegalStateException.class,
				() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));
	}

	@Test
	void productionCoordinatesNeverFallBackToLocalDefaults() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_HOST", "mysql.example")
				.withProperty("DB_PORT", "22820");
		environment.setActiveProfiles("production");

		assertThrows(
				IllegalStateException.class,
				() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));
	}

	@Test
	void productionRejectsLocalhostDatabaseUrl() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DB_HOST", "localhost")
				.withProperty("DB_PORT", "3306")
				.withProperty("DB_NAME", "tutotext_db");
		environment.setActiveProfiles("production");

		assertThrows(
				IllegalStateException.class,
				() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));
	}

	@Test
	void explicitDatasourceUrlIsNotOverwritten() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
				"explicitTestDatasource",
				java.util.Map.of("spring.datasource.url", "jdbc:h2:mem:existing-test")));

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("jdbc:h2:mem:existing-test", environment.getProperty("spring.datasource.url"));
		assertNull(environment.getProperty("SPRING_DATASOURCE_URL"));
	}
}
