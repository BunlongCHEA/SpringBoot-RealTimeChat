package com.project.realtimechat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.project.realtimechat.filter.JwtAuthenticationFilter;
import com.project.realtimechat.service.UserService;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    // Note: UserService must implement UserDetailsService
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
        		.requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()  // Allow WebSocket handshake
                .requestMatchers("/app/**").permitAll()  // Allow STOMP app destinations
                .requestMatchers("/topic/**").permitAll()  // Allow STOMP topic destinations
                .requestMatchers("/queue/**").permitAll()  // Allow STOMP queue destinations
                .requestMatchers("/user/**").permitAll()  // Allow STOMP user destinations
                .requestMatchers("/error/**").permitAll()  // Allow error endpoints
                .requestMatchers("/actuator/**").permitAll()  // Allow actuator endpoints
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // The no-args constructor is deprecated, but still works
        // You can also use the constructor that takes the userDetailsService directly
    	DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        
        // Cast UserService to UserDetailsService
        provider.setUserDetailsService((UserDetailsService)userService);
        provider.setPasswordEncoder(passwordEncoder());
        
        return provider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(authenticationProvider()));
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}