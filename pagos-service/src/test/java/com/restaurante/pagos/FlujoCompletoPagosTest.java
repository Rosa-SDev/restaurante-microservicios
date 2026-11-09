package com.restaurante.pagos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del flujo completo de pagos:
 * 1. POST válido → 201, estado PENDIENTE, con referencia.
 * 2. POST sin monto → 400 con el campo monto en campos.
 * 3. GET por id → 200. GET de un id que no existe → 404.
 * 4. PUT .../estado a APROBADO → 200.
 * 5. PUT .../estado del mismo pago a RECHAZADO → 409.
 * 6. Otro POST para la misma factura → 409.
 * 7. DELETE del pago aprobado → 409.
 * 8. POST de otra factura, DELETE → 204, GET → 404.
 */
@SpringBootTest
class FlujoCompletoPagosTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void flujoCompletoDePagos() throws Exception {
        // 1. POST válido → 201, estado PENDIENTE, con referencia
        mockMvc.perform(post("/api/pagos")
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
                .andExpect(jsonPath("$.registradoPor", equalTo("sistema")));

        // 2. POST sin monto → 400 con el campo monto en campos
        mockMvc.perform(post("/api/pagos")
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
        mockMvc.perform(get("/api/pagos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.estado", equalTo("PENDIENTE")));

        mockMvc.perform(get("/api/pagos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("El pago 999 no existe.")));

        // 4. PUT .../estado a APROBADO → 200
        mockMvc.perform(put("/api/pagos/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "estado": "APROBADO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", equalTo("APROBADO")));

        // Comprobar listado con filtros opcionales
        mockMvc.perform(get("/api/pagos?estado=APROBADO&facturaId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 5. PUT .../estado del mismo pago a RECHAZADO → 409
        mockMvc.perform(put("/api/pagos/1/estado")
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
        mockMvc.perform(delete("/api/pagos/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("Un pago aprobado no se puede eliminar.")));

        // 8. POST de otra factura, DELETE → 204, GET → 404
        mockMvc.perform(post("/api/pagos")
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

        mockMvc.perform(delete("/api/pagos/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pagos/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("El pago 2 no existe.")));
    }
}
