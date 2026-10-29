package com.tutotext.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AbuseProtectionConfiguration {

	@Bean
	FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilterRegistration(
			RequestBodyLimitFilter filter) {
		FilterRegistrationBean<RequestBodyLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
