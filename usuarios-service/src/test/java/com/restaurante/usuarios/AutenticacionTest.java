package com.restaurante.usuarios;

import com.jayway.jsonpath.JsonPath;
import com.restaurante.usuarios.config.SeguridadConfig;
import com.restaurante.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del login con token: forma de la respuesta, token RS256 verificable con la clave pública,
 * rechazo de una clave pública ajena y contraseñas guardadas con BCrypt.
 */
@SpringBootTest
class AutenticacionTest {
    private static final String CORREO_ADMIN = "administrador@restaurante.com";

    @DynamicPropertySource
    static void claves(DynamicPropertyRegistry registro) {
        ClavesRsaTest.registrar(registro);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UsuarioRepository repositorio;

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

    private String loginDelAdministrador() throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO_ADMIN, "admin123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
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
    void elTokenSeVerificaConLaClavePublicaYLlevaLosClaimsEsperados() throws Exception {
        String token = loginDelAdministrador();

        // Mismo mecanismo que usará el gateway: solo la clave pública, RS256
        Jwt jwt = NimbusJwtDecoder.withPublicKey(ClavesRsaTest.clavePublica())
                .build()
                .decode(token);

        assertEquals("RS256", jwt.getHeaders().get("alg"));
        assertEquals(SeguridadConfig.KID, jwt.getHeaders().get("kid"));
        assertEquals("ADMINISTRADOR", jwt.getClaimAsString("rol"));
        assertEquals(CORREO_ADMIN, jwt.getSubject());
        assertEquals("usuarios-service", jwt.getClaimAsString("iss"));
        assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        assertTrue(jwt.hasClaim("uid"));
    }

    @Test
    void unaClavePublicaAjenaRechazaElToken() throws Exception {
        String token = loginDelAdministrador();

        JwtDecoder decodificadorAjeno = NimbusJwtDecoder
                .withPublicKey(ClavesRsaTest.otraClavePublica())
                .build();

        assertThrows(JwtException.class, () -> decodificadorAjeno.decode(token));
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
