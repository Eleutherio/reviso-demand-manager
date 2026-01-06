package com.guilherme.reviso_demand_manager.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/", "/index.html").permitAll()
                
                // Admin endpoints - only AGENCY_ADMIN
                .requestMatchers("/admin/**").hasRole("AGENCY_ADMIN")
                
                // Agency endpoints - AGENCY_ADMIN and AGENCY_USER
                .requestMatchers("/agency/**").hasAnyRole("AGENCY_ADMIN", "AGENCY_USER")
                
                // Briefings endpoints - CLIENT_USER
                .requestMatchers(HttpMethod.POST, "/briefings").hasRole("CLIENT_USER")
                .requestMatchers(HttpMethod.GET, "/briefings/mine").hasRole("CLIENT_USER")
                
                // Requests endpoints - CLIENT_USER for /mine
                .requestMatchers(HttpMethod.GET, "/requests/mine").hasRole("CLIENT_USER")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
