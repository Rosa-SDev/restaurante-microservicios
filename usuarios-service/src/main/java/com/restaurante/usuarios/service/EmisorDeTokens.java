package com.restaurante.usuarios.service;

import com.restaurante.usuarios.dto.UsuarioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Firma el JWT que el gateway va a validar. Solo recibe el usuarioDTO (sin el hash de password)
 */
@Service
public class EmisorDeTokens {
    private final JwtEncoder jwtEncoder;
    private final long expiracionSegundos;

    public EmisorDeTokens(JwtEncoder jwtEncoder, @Value("${seguridad.jwt.expiracion-segundos}") long expiracionSegundos) {
        this.jwtEncoder = jwtEncoder;
        this.expiracionSegundos = expiracionSegundos;
    }

    public String emitir(UsuarioDTO usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("usuarios-service")
                .subject(usuario.correo())
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expiracionSegundos))
                .claim("uid", usuario.id())
                .claim("rol", usuario.rol().name())
                .build();

        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(encabezado, claims)).getTokenValue();
    }

    public long getExpiracionSegundos() {
        return expiracionSegundos;
    }
}
