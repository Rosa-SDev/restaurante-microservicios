package com.restaurante.gateway.seguridad;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del gateway: valida el token de cada petición y decide quién puede
 * entrar a cada ruta antes de reenviarla.
 *
 * Hay dos cadenas porque hay dos emisores y cada ruta confía en uno solo:
 * - /api/pagos/** acepta solo tokens de Keycloak, con el scope que pide cada
 *   verbo. Lleva @Order(1) y un securityMatcher, así que se evalúa primero y
 *   solo atiende esas rutas.
 * - Todo lo demás acepta solo tokens de usuarios-service, con los permisos por
 *   rol. Lleva @Order(2) y atiende lo que la primera no tomó.
 * Un token de un emisor en la ruta del otro no pasa la firma y recibe 401.
 *
 * Los decodificadores y los conversores no son @Bean a propósito. Si lo fueran,
 * habría dos JwtDecoder en el contexto y Spring no sabría cuál inyectar; peor
 * aún, una cadena que no fijara el suyo tomaría del contexto el de la otra (por
 * ejemplo, el conversor del claim rol aplicado a los tokens de Keycloak, que no
 * lo traen, dejaría todo en 403). Cada cadena construye los suyos y los pasa
 * explícitamente.
 *
 * Los tokens de usuarios-service se firman con RS256: usuarios-service usa su
 * clave privada y el gateway solo conoce la pública, que llega por la propiedad
 * seguridad.jwt.clave-publica. Con esa clave el gateway puede comprobar un
 * token, pero no fabricarlo. En el perfil docker las propiedades de seguridad
 * salen de variables de entorno sin valor por omisión: si falta una, el gateway
 * no arranca.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    /**
     * Valor del claim iss en los tokens que firma usuarios-service.
     */
    private static final String EMISOR_USUARIOS = "usuarios-service";

    /**
     * Audiencia que deben traer en el claim aud los tokens de Keycloak.
     */
    private static final String AUDIENCIA_PAGOS = "pagos-api";

    private final RSAPublicKey clavePublica;

    private final String emisorKeycloak;

    private final String jwksKeycloak;

    private final RespuestasDeSeguridad respuestas;

    /**
     * Spring Security convierte por sí solo la propiedad en una RSAPublicKey: lee
     * el archivo PEM (file: o classpath:) en formato X.509 BEGIN PUBLIC KEY.
     */
    public SeguridadConfig(@Value("${seguridad.jwt.clave-publica}") RSAPublicKey clavePublica,
                           @Value("${seguridad.oauth2.emisor}") String emisorKeycloak,
                           @Value("${seguridad.oauth2.jwks}") String jwksKeycloak,
                           RespuestasDeSeguridad respuestas) {
        this.clavePublica = clavePublica;
        this.emisorKeycloak = emisorKeycloak;
        this.jwksKeycloak = jwksKeycloak;
        this.respuestas = respuestas;
    }

    /**
     * Cadena de pagos: solo tokens de Keycloak. GET pide el scope pagos:leer;
     * POST, PUT y DELETE piden pagos:escribir. Cualquier otro verbo se niega,
     * porque pagos-service no lo atiende.
     *
     * Usa el conversor por omisión de Spring, que convierte el claim scope en
     * autoridades SCOPE_, no el del claim rol. Los manejadores de error son los
     * mismos de la cadena general, por la misma razón que se explica allí.
     */
    @Bean
    @Order(1)
    SecurityFilterChain cadenaDePagos(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/pagos/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(HttpMethod.GET, "/api/pagos/**").hasAuthority("SCOPE_pagos:leer")
                        .requestMatchers(HttpMethod.POST, "/api/pagos/**").hasAuthority("SCOPE_pagos:escribir")
                        .requestMatchers(HttpMethod.PUT, "/api/pagos/**").hasAuthority("SCOPE_pagos:escribir")
                        .requestMatchers(HttpMethod.DELETE, "/api/pagos/**").hasAuthority("SCOPE_pagos:escribir")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(token -> token
                                .decoder(decodificadorDeKeycloak())
                                .jwtAuthenticationConverter(new JwtAuthenticationConverter()))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    /**
     * El orden de las reglas importa, porque gana la primera que coincide con la
     * petición. Por eso el borrado de una línea de pedido va antes de la regla
     * general de DELETE: si estuviera después, esa regla se lo comería y le
     * exigiría el rol ADMINISTRADOR.
     *
     * Los manejadores de error se registran en dos sitios a propósito. El 401 de
     * "no llegó ningún token" lo produce el filtro de autorización y lo atiende
     * exceptionHandling; el 401 de "firma inválida o token vencido" lo produce
     * antes el filtro del token, que usa su propio manejador, el del servidor de
     * recursos. Registrándolo en ambos, los cuatro caminos devuelven el mismo
     * cuerpo.
     */
    @Bean
    @Order(2)
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/pedidos/*/platillos/*").authenticated()
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(token -> token
                                .decoder(decodificadorDeUsuarios())
                                .jwtAuthenticationConverter(convertidorDeRol()))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    /**
     * Comprueba la firma RS256 con la clave pública, el vencimiento y que el
     * emisor sea usuarios-service. La clave pública se puede repartir sin
     * riesgo, así que el emisor es una segunda comprobación de que el token salió
     * de donde debe.
     */
    private JwtDecoder decodificadorDeUsuarios() {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withPublicKey(clavePublica)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR_USUARIOS));
        return decodificador;
    }

    /**
     * Comprueba los tokens de Keycloak: la firma con las claves públicas del
     * JWKS, el vencimiento, el emisor y que la audiencia incluya pagos-api.
     *
     * El emisor se compara con la URL pública de Keycloak, que es la que queda en
     * el claim iss, mientras que el JWKS se descarga por la URL que el gateway
     * alcance (la interna en Docker y Kubernetes). La descarga ocurre con el
     * primer token, no al arrancar, y luego las claves quedan en memoria.
     */
    private JwtDecoder decodificadorDeKeycloak() {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(jwksKeycloak).build();
        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(AUDIENCIA_PAGOS));
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(emisorKeycloak), audiencia));
        return decodificador;
    }

    /**
     * Convierte el claim "rol" del token en la autoridad ROLE_ más el rol, que es
     * la forma que espera hasRole.
     */
    private JwtAuthenticationConverter convertidorDeRol() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }
}
