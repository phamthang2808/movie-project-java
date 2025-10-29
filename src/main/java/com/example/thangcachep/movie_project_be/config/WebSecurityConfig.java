package com.example.thangcachep.movie_project_be.config;


import static org.springframework.http.HttpMethod.*;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.thangcachep.movie_project_be.filters.JwtTokenFilter;

import lombok.RequiredArgsConstructor;

@Configuration
//@EnableMethodSecurity
@EnableWebSecurity(debug = true)
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@EnableWebMvc
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final FileUploadProperties fileUploadProperties;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> {
                    requests
                            .requestMatchers(
                                    String.format("%s/users/login", apiPrefix),
                                    String.format("%s/users/register", apiPrefix)
                            )
                            .permitAll()
                            .requestMatchers(HttpMethod.GET,
                                    String.format("%s/roles**",apiPrefix)).permitAll()



//                           permitAll = không cần đăng nhập, không cần token.
//
//                           authenticated = cần đăng nhập, cần token hợp lệ (role gì cũng được).
//
//                           hasRole("STAFF") = cần token hợp lệ + chứa role STAFF.
                            
                            .requestMatchers(String.format("%s/admins/**", apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/staff/**", apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/customers/**", apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/rooms/**", apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/users/**",apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/bookings/**",apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/reviews/**",apiPrefix)).permitAll()
                            .requestMatchers(String.format("%s/reply/**",apiPrefix)).permitAll()

                            .requestMatchers(GET,
                                    String.format("%s/healthcheck/**", apiPrefix)).permitAll()
                            .requestMatchers("/error", "/favicon.ico", "/css/**", "/js/**", "/images/**").permitAll()
                            .requestMatchers("/" + fileUploadProperties.getUploadDir() + "/**").permitAll()
                            .requestMatchers("/api/" + fileUploadProperties.getUploadDir() + "/**").permitAll()

//                            .requestMatchers(String.format("%s/rooms/**", apiPrefix)).hasRole("STAFF")
//                            .requestMatchers(HttpMethod.POST, String.format("%s/rooms/**", apiPrefix)).hasRole("STAFF")
//                            .requestMatchers(HttpMethod.PUT, String.format("%s/rooms/**", apiPrefix)).hasRole("STAFF")
//                            .requestMatchers(HttpMethod.DELETE, String.format("%s/rooms/**", apiPrefix)).hasRole("STAFF")

//                            .requestMatchers(String.format("%s/users/**", apiPrefix)).hasRole("STAFF")

                            // ====== ADMIN ======
//                            .requestMatchers(String.format("%s/roles/**", apiPrefix)).hasRole("ADMIN")

                            .anyRequest().authenticated();
                })
                .csrf(AbstractHttpConfigurer::disable);
        http.cors(new Customizer<CorsConfigurer<HttpSecurity>>() {
            @Override
            public void customize(CorsConfigurer<HttpSecurity> httpSecurityCorsConfigurer) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
                configuration.setExposedHeaders(List.of("x-auth-token"));
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                httpSecurityCorsConfigurer.configurationSource(source);
            }
        });
        return http.build();
    }
}
