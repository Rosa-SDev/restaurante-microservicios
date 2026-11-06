package com.restaurante.gateway.seguridad;

import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
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
 * con RS256. usuarios-service firma con su clave privada y el gateway solo
 * conoce la pública, que llega por la propiedad seguridad.jwt.clave-publica.
 * Con esa clave el gateway puede comprobar un token, pero no fabricarlo. En el
 * perfil docker la ruta sale de la variable JWT_CLAVE_PUBLICA, que no tiene
 * valor por omisión: si falta, el gateway no arranca.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final RSAPublicKey clavePublica;

    private final RespuestasDeSeguridad respuestas;

    /**
     * Spring Security convierte por sí solo la propiedad en una RSAPublicKey: lee
     * el archivo PEM (file: o classpath:) en formato X.509 BEGIN PUBLIC KEY.
     */
    public SeguridadConfig(@Value("${seguridad.jwt.clave-publica}") RSAPublicKey clavePublica,
                           RespuestasDeSeguridad respuestas) {
        this.clavePublica = clavePublica;
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
     * Comprueba la firma RS256 con la clave pública y el vencimiento, que es el
     * validador que trae por omisión.
     */
    private JwtDecoder decodificadorDeUsuarios() {
        return NimbusJwtDecoder.withPublicKey(clavePublica)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
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
