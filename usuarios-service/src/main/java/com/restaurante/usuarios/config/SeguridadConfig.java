package com.restaurante.usuarios.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SeguridadConfig {
    public static final String KID = "restaurante-usuarios-1";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${seguridad.jwt.clave-privada}") Resource clavePrivada, @Value("${seguridad.jwt.clave-publica}") Resource clavePublica) {
        RSAKey claveRsa = new RSAKey.Builder(LectorClavesPEM.leerClavePublica(clavePublica))
                .privateKey(LectorClavesPEM.leerClavePrivada(clavePrivada))
                .keyID(KID)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(claveRsa)));
    }
}