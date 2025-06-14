package com.example.Hospital.security.config;

import com.example.Hospital.security.user.Permission;
import com.example.Hospital.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private static final String[] WHITE_LIST_URL = {
            "/api/v1/auth/**",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html",
            "/ws/**",  // Add WebSocket endpoint
            "/ws"      // Add WebSocket endpoint without wildcard
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(req ->
                        req.requestMatchers(WHITE_LIST_URL).permitAll()
                                .requestMatchers("/api/v1/management/**").hasAnyRole(Role.ADMIN.name(), Role.MANAGER.name())
                                .requestMatchers(GET, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_READ.name(), Permission.MANAGER_READ.name(), Permission.THERAPIST_READ.name())
                                .requestMatchers(POST, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_CREATE.name(), Permission.MANAGER_CREATE.name(), Permission.THERAPIST_CREATE.name())
                                .requestMatchers(PUT, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_UPDATE.name(), Permission.MANAGER_UPDATE.name(), Permission.THERAPIST_UPDATE.name())
                                .requestMatchers(DELETE, "/api/v1/management/**").hasAnyAuthority(Permission.ADMIN_DELETE.name(), Permission.MANAGER_DELETE.name(), Permission.THERAPIST_DELETE.name())
                                .requestMatchers(GET, "/api/v1/auth/**").hasAnyAuthority(Permission.ADMIN_READ.name(), Permission.MANAGER_READ.name(), Permission.THERAPIST_READ.name())
                                .requestMatchers(PATCH, "/api/v1/users/change-password").authenticated()
                                // Payment endpoints security
                                .requestMatchers(GET, "/api/users/{userId}/credits").authenticated()
                                .requestMatchers(POST, "/api/payments/process").authenticated()
                                .requestMatchers("/api/v1/payments/**").authenticated()
                                // Chat endpoints security
                                .requestMatchers("/api/v1/chat/**").authenticated()
                                .requestMatchers(GET, "/api/v1/chat/rooms").authenticated()
                                .requestMatchers(POST, "/api/v1/chat/rooms/create").authenticated()
                                .requestMatchers(GET, "/api/v1/chat/messages/**").authenticated()
                                .requestMatchers(POST, "/api/v1/chat/messages/send").authenticated()
                                .requestMatchers(PUT, "/api/v1/chat/messages/*/read").authenticated()
                                .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
                );

        return http.build();
    }
}