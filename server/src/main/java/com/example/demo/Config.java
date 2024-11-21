package com.example.demo;
import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class Config{
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(request -> {
                  CorsConfiguration configuration = new CorsConfiguration();
                  configuration.setAllowedOrigins(Arrays.asList("*"));
                  configuration.setAllowedMethods(Arrays.asList("*"));
                  configuration.setAllowedHeaders(Arrays.asList("*"));
                  return configuration;
        }))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }
}
