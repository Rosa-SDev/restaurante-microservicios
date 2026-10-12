package com.restaurante.usuarios;

import com.jayway.jsonpath.JsonPath;
import com.restaurante.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del login con token: forma de la respuesta, token verificable con la clave compartida,
 * rechazo de claves incorrectas y contraseñas guardadas con BCrypt.
 */
@SpringBootTest
class AutenticacionTest {
    private static final String CORREO_ADMIN = "administrador@restaurante.com";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UsuarioRepository repositorio;

    @Value("${seguridad.jwt.secreto}")
    private String secreto;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    private String cuerpoLogin(String correo, String password) {
        return """
                {
                    "correo": "%s",
                    "password": "%s"
                }
                """.formatted(correo, password);
    }

    @Test
    void loginDelAdministradorDevuelveTokenYUsuarioSinHash() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO_ADMIN, "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tipo", equalTo("Bearer")))
                .andExpect(jsonPath("$.expiraEn", equalTo(3600)))
                .andExpect(jsonPath("$.usuario.correo", equalTo(CORREO_ADMIN)))
                .andExpect(jsonPath("$.usuario.rol", equalTo("ADMINISTRADOR")))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void elTokenSeVerificaConLaClaveCompartidaYLlevaLosClaimsEsperados() throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO_ADMIN, "admin123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(respuesta, "$.token");

        // Mismo mecanismo que usa el gateway: clave HMAC a partir de los bytes UTF-8 del secreto
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Jwt jwt = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(token);

        assertEquals("ADMINISTRADOR", jwt.getClaimAsString("rol"));
        assertEquals(CORREO_ADMIN, jwt.getSubject());
        assertEquals("usuarios-service", jwt.getClaimAsString("iss"));
        assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        assertTrue(jwt.hasClaim("uid"));
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO_ADMIN, "clave-equivocada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void lasContraseñasSeGuardanConBCrypt() {
        String hash = repositorio.findByCorreoIgnoreCase(CORREO_ADMIN)
                .orElseThrow()
                .getPasswordHash();

        assertTrue(hash.startsWith("$2a$"), "El hash guardado debe ser BCrypt");
        assertTrue(!hash.contains("admin123"), "La contraseña no puede guardarse en claro");
    }
}
