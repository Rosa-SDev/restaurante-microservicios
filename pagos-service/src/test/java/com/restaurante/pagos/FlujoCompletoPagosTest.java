package com.restaurante.pagos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del flujo completo de pagos y del servidor de recursos OAuth 2.0.
 */
@SpringBootTest
class FlujoCompletoPagosTest {

    private static final String SIN_TOKEN_VALIDO = "Debe iniciar sesión con un token válido.";
    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta operación.";

    private static final SimpleGrantedAuthority SCOPE_LEER =
            new SimpleGrantedAuthority("SCOPE_pagos:leer");
    private static final SimpleGrantedAuthority SCOPE_ESCRIBIR =
            new SimpleGrantedAuthority("SCOPE_pagos:escribir");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE pagos RESTART IDENTITY");
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/pagos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo(SIN_TOKEN_VALIDO)));
    }

    @Test
    void postConSoloScopeLeerDevuelve403() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .with(jwt().authorities(SCOPE_LEER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 1,
                                    "monto": 58000.00,
                                    "metodo": "TARJETA"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo(SIN_PERMISOS)));
    }

    @Test
    void postConScopeEscribirGuardaPreferredUsernameDelToken() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account-pagos-cliente"))
                                .authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 1,
                                    "monto": 58000.00,
                                    "metodo": "TARJETA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradoPor", equalTo("service-account-pagos-cliente")));
    }

    @Test
    void postSinPreferredUsernameUsaAzpDelToken() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .with(jwt()
                                .jwt(token -> token.claim("azp", "pagos-cliente"))
                                .authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 1,
                                    "monto": 58000.00,
                                    "metodo": "TARJETA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradoPor", equalTo("pagos-cliente")));
    }

    @Test
    void getConScopeLeerDevuelve200() throws Exception {
        mockMvc.perform(get("/api/pagos")
                        .with(jwt().authorities(SCOPE_LEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rutaFueraDePagosQuedaDenegadaPorDefecto() throws Exception {
        mockMvc.perform(get("/api/otra-ruta")
                        .with(jwt().authorities(SCOPE_LEER, SCOPE_ESCRIBIR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo(SIN_PERMISOS)));
    }

    @Test
    void flujoCompletoDePagos() throws Exception {
        // 1. POST válido → 201, estado PENDIENTE, con referencia
        mockMvc.perform(post("/api/pagos")
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account-pagos-cliente"))
                                .authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 1,
                                    "monto": 58000.00,
                                    "metodo": "TARJETA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.facturaId", equalTo(1)))
                .andExpect(jsonPath("$.monto", equalTo(58000.0)))
                .andExpect(jsonPath("$.metodo", equalTo("TARJETA")))
                .andExpect(jsonPath("$.estado", equalTo("PENDIENTE")))
                .andExpect(jsonPath("$.referencia", notNullValue()))
                .andExpect(jsonPath("$.registradoPor", equalTo("service-account-pagos-cliente")));

        // 2. POST sin monto → 400 con el campo monto en campos
        mockMvc.perform(post("/api/pagos")
                        .with(jwt().authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 1,
                                    "metodo": "EFECTIVO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("La petición tiene campos inválidos")))
                .andExpect(jsonPath("$.campos.monto", equalTo("El monto es obligatorio")));

        // 3. GET por id → 200. GET de un id que no existe → 404
        mockMvc.perform(get("/api/pagos/1")
                        .with(jwt().authorities(SCOPE_LEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.estado", equalTo("PENDIENTE")));

        mockMvc.perform(get("/api/pagos/999")
                        .with(jwt().authorities(SCOPE_LEER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("El pago 999 no existe.")));

        // 4. PUT .../estado a APROBADO → 200
        mockMvc.perform(put("/api/pagos/1/estado")
                        .with(jwt().authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "estado": "APROBADO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", equalTo("APROBADO")));

        // Comprobar listado con filtros opcionales
        mockMvc.perform(get("/api/pagos?estado=APROBADO&facturaId=1")
                        .with(jwt().authorities(SCOPE_LEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 5. PUT .../estado del mismo pago a RECHAZADO → 409
        mockMvc.perform(put("/api/pagos/1/estado")
                        .with(jwt().authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "estado": "RECHAZADO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("No se puede cambiar un pago de APROBADO a RECHAZADO.")));

        // 6. Otro POST para la misma factura → 409
        mockMvc.perform(post("/api/pagos")
                        .with(jwt().authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 1,
                                    "monto": 58000.00,
                                    "metodo": "EFECTIVO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("La factura 1 ya tiene un pago aprobado.")));

        // 7. DELETE del pago aprobado → 409
        mockMvc.perform(delete("/api/pagos/1")
                        .with(jwt().authorities(SCOPE_ESCRIBIR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("Un pago aprobado no se puede eliminar.")));

        // 8. POST de otra factura, DELETE → 204, GET → 404
        mockMvc.perform(post("/api/pagos")
                        .with(jwt().authorities(SCOPE_ESCRIBIR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "facturaId": 2,
                                    "monto": 32000.00,
                                    "metodo": "TRANSFERENCIA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", equalTo(2)));

        mockMvc.perform(delete("/api/pagos/2")
                        .with(jwt().authorities(SCOPE_ESCRIBIR)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pagos/2")
                        .with(jwt().authorities(SCOPE_LEER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("El pago 2 no existe.")));
    }
}
