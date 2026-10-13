package com.restaurante.gateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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
 * 2. GET /api/mesas con un token firmado con otra clave → 401.
 * 3. GET /api/usuarios con rol MESERO → 403, porque esa ruta es solo del
 *    ADMINISTRADOR.
 * 4. DELETE /api/platillos/1 con rol MESERO → 403, porque los borrados bajo
 *    /api/** son solo del ADMINISTRADOR.
 *
 * No se prueba ningún caso con permiso suficiente: el gateway intentaría
 * reenviar la petición a un servicio que no está corriendo.
 */
@SpringBootTest
class SeguridadGatewayTest {

    private static final String SIN_TOKEN_VALIDO = "Debe iniciar sesión con un token válido.";

    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta operación.";

    private static final String OTRA_CLAVE = "clave-distinta-que-el-gateway-no-conoce-2026";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("${seguridad.jwt.secreto}")
    private String secreto;

    private MockMvc mockMvc;

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
        String token = token(OTRA_CLAVE, "ADMINISTRADOR");

        mockMvc.perform(get("/api/mesas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo(SIN_TOKEN_VALIDO)));
    }

    @Test
    void meseroNoEntraAUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + token(secreto, "MESERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo(SIN_PERMISOS)));
    }

    @Test
    void meseroNoBorraPlatillos() throws Exception {
        mockMvc.perform(delete("/api/platillos/1")
                        .header("Authorization", "Bearer " + token(secreto, "MESERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo(SIN_PERMISOS)));
    }

    /**
     * Firma un token HS256 con los mismos claims que emite usuarios-service. La
     * clave llega por parámetro para poder probar también la firma inválida.
     */
    private String token(String clave, String rol) {
        SecretKeySpec llave = new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        ImmutableSecret<SecurityContext> fuente = new ImmutableSecret<>(llave);
        JwtEncoder codificador = new NimbusJwtEncoder(fuente);

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("usuarios-service")
                .subject("mesero@restaurante.com")
                .claim("uid", 2)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(1, ChronoUnit.HOURS))
                .build();

        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
