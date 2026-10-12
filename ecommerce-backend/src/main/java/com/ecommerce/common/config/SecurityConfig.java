package com.ecommerce.common.config;

import com.ecommerce.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        http.csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                // Registration and login are public.
                .requestMatchers("/api/auth/**").permitAll()

                // Public product and category browsing.
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/products", "/api/products/**",
                        "/api/categories", "/api/categories/**"
                ).permitAll()

                // A user may access their own profile.
                .requestMatchers(
                        HttpMethod.GET, "/api/users/me"
                ).authenticated()

                // User administration is restricted to admins.
                .requestMatchers("/api/users", "/api/users/**")
                .hasRole("ADMIN")

                // Product and category modifications are admin-only.
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/products", "/api/products/**",
                        "/api/categories", "/api/categories/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/products", "/api/products/**",
                        "/api/categories", "/api/categories/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/products", "/api/products/**",
                        "/api/categories", "/api/categories/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/products", "/api/products/**",
                        "/api/categories", "/api/categories/**"
                ).hasRole("ADMIN")

                // Inventory management is admin-only.
                .requestMatchers("/api/inventory", "/api/inventory/**")
                .hasRole("ADMIN")

                // All remaining endpoints require authentication.
                .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // Prevent the @Component filter from also being registered
    // as a standalone servlet filter. It should run through
    // Spring Security's filter chain.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
    jwtFilterServletRegistration(
            JwtAuthenticationFilter filter) {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);
        return registration;
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

}
