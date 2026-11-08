package com.example.sapir.webstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                "/", "/index.html", "/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products", "/api/products/**", "/api/categories", "/api/categories/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/auth/me")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/index.html"));
        return http.build();
    }
}
