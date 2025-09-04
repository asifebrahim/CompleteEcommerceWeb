package com.example.EcommerceFresh.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 🔹 CSRF protection disabled
                .authorizeHttpRequests(auth -> auth
                        // Allow access to static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/productImages/**", "/static/**", "/webjars/**").permitAll()
                        // Allow access to public pages
                        .requestMatchers("/", "/shop/**", "/register/**", "/forgotpassword/**", "/resetPassword/**", "/resetPassword", "/resetPasswordConfirmation", "/saveNewPass", "/verify", "/resend-otp").permitAll()
                        // Allow access to login page
                        .requestMatchers("/login", "/oauth2/**").permitAll()
                        // Admin access
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Protected user areas
                        .requestMatchers("/profile/**", "/cart/**", "/checkout/**", "/wishlist/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/shop", true)
                       .failureUrl("/login?error=true")
                                // .failureHandler(authFailureHandler)
                        .permitAll()
                        .usernameParameter("email")
                        .passwordParameter("password")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
