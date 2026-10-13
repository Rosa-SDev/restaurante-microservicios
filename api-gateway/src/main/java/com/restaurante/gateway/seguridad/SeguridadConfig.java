package com.restaurante.gateway.seguridad;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del gateway: valida el token que emite usuarios-service y decide
 * quién puede entrar a cada ruta antes de reenviar la petición.
 *
 * El token viaja en la cabecera Authorization, con el prefijo Bearer, firmado
 * con HS256. La clave son los bytes UTF-8 de la propiedad seguridad.jwt.secreto.
 * En el perfil docker ese valor sale de la variable JWT_SECRET, que no tiene
 * valor por omisión: si falta, el gateway no arranca.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final String secreto;

    private final RespuestasDeSeguridad respuestas;

    public SeguridadConfig(@Value("${seguridad.jwt.secreto}") String secreto,
                           RespuestasDeSeguridad respuestas) {
        this.secreto = secreto;
        this.respuestas = respuestas;
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
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtDecoder decodificador,
                                          JwtAuthenticationConverter convertidor) throws Exception {
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
                                .decoder(decodificador)
                                .jwtAuthenticationConverter(convertidor))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    /**
     * Comprueba la firma HS256 y el vencimiento, que son los validadores que trae
     * por omisión.
     */
    @Bean
    JwtDecoder decodificadorDeTokens() {
        SecretKeySpec clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Convierte el claim "rol" del token en la autoridad ROLE_ más el rol, que es
     * la forma que espera hasRole.
     */
    @Bean
    JwtAuthenticationConverter convertidorDeRol() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }
}
