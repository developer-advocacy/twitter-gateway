package com.joshlong.twitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.clients.ClientService;
import com.joshlong.twitter.registrations.TwitterRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author Josh Long
 */
@Slf4j
@EnableConfigurationProperties(TwitterProperties.class)
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	WebClient webClient(WebClient.Builder webClient) {
		return webClient.build();
	}

	@Bean
	ApplicationRunner environmentLogger() {
		return args -> System.getenv().forEach((k, v) -> log.info(k + "=" + v));
	}

	@EventListener
	public void liveness(AvailabilityChangeEvent<LivenessState> live) {
		log.info("liveness: " + live.toString());
	}

	@EventListener
	public void readiness(AvailabilityChangeEvent<ReadinessState> live) {
		log.info("readiness: " + live.toString());
	}

	@Bean
	TwitterApiIntegration integration(
			@Value("${spring.security.oauth2.client.registration.twitter.client-id}") String clientId,
			@Value("${spring.security.oauth2.client.registration.twitter.client-secret}") String clientSecret,
			WebClient http, ClientService clientService, ObjectMapper om, TwitterRegistrationService registrations) {
		return new TwitterApiIntegration(clientId, clientSecret, http, clientService, registrations, om);
	}

}

@Controller
@ResponseBody
class DefaultHelloController {

	@GetMapping("/")
	Map<String, String> hello() {
		return Map.of("message", "Hello, world!");
	}

}

/**
 * @author Rob Winch
 */
@Slf4j
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

	@Bean
	TextEncryptor encryptor(TwitterProperties properties) {
		return Encryptors.text(properties.encryption().password(), properties.encryption().salt());
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	DefaultSecurityFilterChain springSecurity(HttpSecurity http, //
			OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository, //
			OAuth2AuthorizationRequestResolver authorizationResolver) throws Exception {
		http//
				.authorizeHttpRequests(requests -> requests //
						.mvcMatchers("/oauth2/authorization", "/register").authenticated() //
						.anyRequest().permitAll())//
				.oauth2Login(oauth2 -> oauth2.authorizedClientRepository(oAuth2AuthorizedClientRepository)
						.authorizationEndpoint(
								authorization -> authorization.authorizationRequestResolver(authorizationResolver)));
		return http.build();
	}

	@Bean
	OAuth2AuthorizationRequestResolver authorizationRequestResolver(
			ClientRegistrationRepository clientRegistrationRepository) {
		var authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
				"/oauth2/authorization");
		authorizationRequestResolver
				.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
		return authorizationRequestResolver;
	}

}
