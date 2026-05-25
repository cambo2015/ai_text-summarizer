package com.aisummarizer.website.config;

import com.aisummarizer.website.services.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    // ✅ Password encoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ AuthenticationProvider bean
    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    // ✅ AuthenticationManager bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/youtube/**").permitAll()
                        .requestMatchers("/api/stripe/webhook").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/summary/**").hasRole("USER")
                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                )

                .headers(headers -> headers

                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self' http://localhost:4200; " +
                                        "script-src 'self' http://localhost:4200 https://js.stripe.com; " +
                                        "style-src 'self' 'unsafe-inline' http://localhost:4200; " +
                                        "img-src 'self' data: https: http://localhost:4200; " +
                                        "font-src 'self' data:; " +
                                        "media-src 'self' blob: data: http://localhost:4200; " +
                                        "worker-src 'self' blob:; " +
                                        "connect-src 'self' http://localhost:4200 ws://localhost:4200 https://api.stripe.com https://*.stripe.com; " +
                                        "frame-src https://js.stripe.com https://hooks.stripe.com https://*.stripe.com; " +
                                        "object-src 'none'; " +
                                        "base-uri 'self'; " +
                                        "form-action 'self';"
                        ))

                        .httpStrictTransportSecurity(hsts ->
                                hsts.includeSubDomains(true).maxAgeInSeconds(31536000))

                        .frameOptions(frame -> frame.sameOrigin())

                        .contentTypeOptions(withDefaults())

                        .referrerPolicy(ref ->
                                ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy",
                                "camera=(), geolocation=(), microphone=(self), payment=(self)"
                        ))

                        .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"))
                        .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Embedder-Policy", "require-corp"))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://192.168.0.43:4200",
                 "https://192.168.0.33:4200",
                "http://frontend",
                "https://frontend"));   // no "*"
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type","Authorization","X-Requested-With","Accept"));
        config.setAllowCredentials(true);                              // 👈 required

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
