package com.restaurante.gateway;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que el gateway rechaza lo que debe rechazar:
 * 1. GET /api/mesas sin token → 401.
 * 2. GET /api/mesas con un token firmado con otra clave privada → 401.
 * 3. GET /api/usuarios con rol MESERO → 403, porque esa ruta es solo del
 *    ADMINISTRADOR.
 * 4. DELETE /api/platillos/1 con rol MESERO → 403, porque los borrados bajo
 *    /api/** son solo del ADMINISTRADOR.
 * 5. GET /api/mesas con un token bien firmado pero de otro emisor → 401.
 *
 * Los tokens se firman con RS256 usando el par RSA de prueba; el gateway recibe
 * la clave pública de ese par por @DynamicPropertySource.
 *
 * No se prueba ningún caso con permiso suficiente: el gateway intentaría
 * reenviar la petición a un servicio que no está corriendo.
 */
@SpringBootTest
class SeguridadGatewayTest {

    private static final String SIN_TOKEN_VALIDO = "Debe iniciar sesión con un token válido.";

    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta operación.";

    private static final String EMISOR_USUARIOS = "usuarios-service";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        ClavesDePrueba.registrar(registro);
    }

    /**
     * El apply(springSecurity()) es imprescindible: sin él MockMvc no monta la
     * cadena de filtros y las peticiones llegarían al enrutador sin pasar por el
     * control de acceso.
     */
    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/mesas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo(SIN_TOKEN_VALIDO)));
    }

    @Test
    void tokenFirmadoConOtraClaveDevuelve401() throws Exception {
        KeyPair otroPar = ClavesDePrueba.generarPar();
        String token = token((RSAPublicKey) otroPar.getPublic(), (RSAPrivateKey) otroPar.getPrivate(),
                EMISOR_USUARIOS, "ADMINISTRADOR");

        mockMvc.perform(get("/api/mesas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo(SIN_TOKEN_VALIDO)));
    }

    @Test
    void meseroNoEntraAUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenDePrueba(EMISOR_USUARIOS, "MESERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo(SIN_PERMISOS)));
    }

    @Test
    void meseroNoBorraPlatillos() throws Exception {
        mockMvc.perform(delete("/api/platillos/1")
                        .header("Authorization", "Bearer " + tokenDePrueba(EMISOR_USUARIOS, "MESERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo(SIN_PERMISOS)));
    }

    @Test
    void tokenDeOtroEmisorDevuelve401() throws Exception {
        mockMvc.perform(get("/api/mesas")
                        .header("Authorization", "Bearer " + tokenDePrueba("otro-emisor", "ADMINISTRADOR")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo(SIN_TOKEN_VALIDO)));
    }

    /**
     * Token firmado con la clave privada del par de prueba, cuya pública conoce
     * el gateway.
     */
    private String tokenDePrueba(String emisor, String rol) {
        return token(ClavesDePrueba.publica(), ClavesDePrueba.privada(), emisor, rol);
    }

    /**
     * Firma un token RS256 con los mismos claims y la misma cabecera que emite
     * usuarios-service. El par llega por parámetro para poder probar también la
     * firma inválida.
     */
    private String token(RSAPublicKey publica, RSAPrivateKey privada, String emisor, String rol) {
        RSAKey clave = new RSAKey.Builder(publica)
                .privateKey(privada)
                .keyID("restaurante-usuarios-1")
                .build();
        JwtEncoder codificador = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(clave)));

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject("mesero@restaurante.com")
                .claim("uid", 2)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(1, ChronoUnit.HOURS))
                .build();

        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId("restaurante-usuarios-1")
                .build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
