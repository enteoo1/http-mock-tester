package com.example.mocktester.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Mock endpoints: open to everyone, no CSRF, no extra security headers, no session. */
    @Bean
    @Order(1)
    public SecurityFilterChain mockChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/mock/**")
                .csrf(c -> c.disable())
                .headers(h -> h.disable())
                .requestCache(rc -> rc.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    /** Web UI. */
    @Bean
    @Order(2)
    public SecurityFilterChain uiChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/register", "/css/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(l -> l.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
