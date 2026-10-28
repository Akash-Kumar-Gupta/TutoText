package com.tutotext.backend.config;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiRestClientConfiguration {

	@Bean
	RestClient.Builder geminiRestClientBuilder() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(10));
		requestFactory.setReadTimeout(Duration.ofSeconds(20));
		return RestClient.builder().requestFactory(requestFactory);
	}
}
