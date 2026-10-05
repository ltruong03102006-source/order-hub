package com.orderhub.config;

import com.orderhub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Công khai Swagger và API Login
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 2. Khách vãng lai xem được danh mục & sản phẩm
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/products/**").permitAll()

                        // 3. Nghiệp vụ Kho & Quản lý: Chỉ ADMIN
                        .requestMatchers("/api/inventories/import").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{orderCode}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/products/**").hasRole("ADMIN")

                        // 4. Nghiệp vụ Khách hàng: CUSTOMER và ADMIN đều được phép
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{orderCode}/cancel").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // 5. Tất cả API còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}