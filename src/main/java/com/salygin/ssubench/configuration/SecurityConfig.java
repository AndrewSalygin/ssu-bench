package com.salygin.ssubench.configuration;

import com.salygin.ssubench.repository.UserRepository;
import com.salygin.ssubench.security.JsonForbiddenHandler;
import com.salygin.ssubench.security.JsonBadRequestEntryPoint;
import com.salygin.ssubench.security.JwtAuthenticationFilter;
import com.salygin.ssubench.security.JwtProperties;
import com.salygin.ssubench.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService tokenService,
            UserRepository users,
            JsonBadRequestEntryPoint authEntryPoint,
            JsonForbiddenHandler deniedHandler
    ) {
        return new JwtAuthenticationFilter(tokenService, users, authEntryPoint, deniedHandler);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity security,
            JwtAuthenticationFilter jwtFilter,
            JsonBadRequestEntryPoint authEntryPoint,
            JsonForbiddenHandler forbiddenHandler
    ) throws Exception {

        security
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(cfg -> cfg.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(cfg -> cfg
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(forbiddenHandler)
                )
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger/**",
                                "/openapi.yaml",
                                "/webjars/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return security.build();
    }
}