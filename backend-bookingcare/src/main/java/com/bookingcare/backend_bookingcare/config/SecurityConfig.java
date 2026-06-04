package com.bookingcare.backend_bookingcare.config;

import com.bookingcare.backend_bookingcare.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origin-patterns}")
    private String allowedOriginPatterns;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/specialties").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/doctors").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/doctors/specialty/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/legal-documents/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/booking/working-dates").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/booking/slots").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/booking/book").hasRole("PATIENT")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/v1/chatbot/**").hasRole("PATIENT")
                        .requestMatchers("/api/v1/doctors/messages/**").hasRole("DOCTOR")
                        .requestMatchers("/api/v1/doctors/profile").hasRole("DOCTOR")
                        .requestMatchers("/api/v1/doctors/appointment-status").hasRole("DOCTOR")
                        .requestMatchers("/api/v1/doctors/appointment-today").hasRole("DOCTOR")
                        .requestMatchers("/api/v1/doctors/appointment/**").hasRole("DOCTOR")
                        .anyRequest().authenticated()
                )
                // Anonymous truy cập endpoint secured → trả 401 (không phải 403 mặc định)
                // để TokenAuthInterceptor trên Android refresh hoặc về màn login.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOriginPatterns.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
