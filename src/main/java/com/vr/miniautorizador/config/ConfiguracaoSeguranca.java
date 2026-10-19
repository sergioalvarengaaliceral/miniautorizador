package com.vr.miniautorizador.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracaoSeguranca {

	@Bean
	SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {

		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(autorizacao -> autorizacao
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(STATELESS));

		return http.build();
	}

	@Bean
	UserDetailsService usuariosDaApi() {
		UserDetails usuario = User.withUsername("username").password("{noop}password").roles("API").build();
		return new InMemoryUserDetailsManager(usuario);
	}

	@Bean
	PasswordEncoder codificadorDeSenha() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
