package com.restaurante.pagos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Servidor de recursos OAuth 2.0 de pagos-service.
 *
 * Exige un token emitido por Keycloak con audiencia pagos-api y verifica los
 * scopes según el verbo HTTP: pagos:leer para consultas y pagos:escribir para
 * escrituras. Cualquier otra ruta se rechaza por defecto con denyAll().
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final RespuestasDeSeguridad respuestas;

    public SeguridadConfig(RespuestasDeSeguridad respuestas) {
        this.respuestas = respuestas;
    }

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(HttpMethod.GET, "/api/pagos/**").hasAuthority("SCOPE_pagos:leer")
                        .requestMatchers(HttpMethod.POST, "/api/pagos/**").hasAuthority("SCOPE_pagos:escribir")
                        .requestMatchers(HttpMethod.PUT, "/api/pagos/**").hasAuthority("SCOPE_pagos:escribir")
                        .requestMatchers(HttpMethod.DELETE, "/api/pagos/**").hasAuthority("SCOPE_pagos:escribir")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }
}
