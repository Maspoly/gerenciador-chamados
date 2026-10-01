package br.com.dunnastecnologia.chamados.integration.controller.web;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import br.com.dunnastecnologia.chamados.infrastructure.security.JwtService;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    JwtService jwtService() {
        return mock(JwtService.class);
    }

    @Bean
    UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("admin@condominio.local")
                .password("senha")
                .authorities("ROLE_ADMINISTRADOR")
                .build();
        return username -> user;
    }

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
