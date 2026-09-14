package com.restaurante.salon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del flujo completo del salón según el Paso 9 de TAREAS-CAMILO.md:
 * 1. POST /api/mesas → crea una mesa LIBRE (201).
 * 2. POST /api/pedidos → abre pedido sobre esa mesa (201) y la mesa queda OCUPADA.
 * 3. POST /api/pedidos/{id}/platillos dos veces → dos líneas agregadas.
 * 4. POST /api/pedidos sobre la misma mesa → 409 Conflict.
 * 5. POST /api/facturas con monto insuficiente → 409 Conflict.
 * 6. POST /api/facturas con monto correcto → 201 Created, pedido PAGADO y mesa vuelve a LIBRE.
 * 7. GET /api/mesas/{id} → confirma estado LIBRE.
 * 8. DELETE /api/facturas/{id} → 200 OK y factura anulada.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoCompletoSalonTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    @Order(1)
    void flujoCompletoMesaPedidoFactura() throws Exception {
        // 1. Crear una mesa 10 con capacidad 4
        mockMvc.perform(post("/api/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "numero": 10,
                                    "capacidad": 4
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", equalTo(10)))
                .andExpect(jsonPath("$.estado", equalTo("LIBRE")));

        // 2. Abrir un pedido en la mesa 10 (Mesa ID 6, ya que hay 5 de demostración)
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "mesaId": 6,
                                    "meseroId": 2,
                                    "clienteId": 1,
                                    "observaciones": "Cerca a la ventana"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", equalTo("ABIERTO")))
                .andExpect(jsonPath("$.mesaNumero", equalTo(10)));

        // Verificar que la mesa 10 ahora está OCUPADA
        mockMvc.perform(get("/api/mesas/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", equalTo("OCUPADA")));

        // 3. Agregar platillos dos veces (dos líneas desnormalizadas)
        // Platillo 1: Hamburguesa 25000.00
        mockMvc.perform(post("/api/pedidos/1/platillos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "platilloId": 101,
                                    "nombre": "Hamburguesa artesanal",
                                    "precioUnitario": 25000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platillos", hasSize(1)))
                .andExpect(jsonPath("$.total", equalTo(25000.0)));

        // Platillo 2: Limonada 5000.00 -> total sin impuestos = 30000.00
        mockMvc.perform(post("/api/pedidos/1/platillos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "platilloId": 102,
                                    "nombre": "Limonada de coco",
                                    "precioUnitario": 5000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platillos", hasSize(2)))
                .andExpect(jsonPath("$.total", equalTo(30000.0)));

        // 4. Intentar abrir otro pedido sobre la misma mesa (mesaId: 6) → 409 Conflict
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "mesaId": 6,
                                    "meseroId": 3,
                                    "observaciones": "Intento duplicado"
                                }
                                """))
                .andExpect(status().isConflict());

        // 5. Intentar emitir factura con monto insuficiente:
        // Consumo = 30000.00, Impuesto 8% = 2400.00, Total = 32400.00.
        // Si entregamos 30000.00 -> debe rechazar con 409 Conflict
        mockMvc.perform(post("/api/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1,
                                    "cajeroId": 5,
                                    "metodoPago": "EFECTIVO",
                                    "monto": 30000.00
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("El monto no cubre el total de la factura ($32400.00).")));

        // 6. Emitir factura con monto suficiente (35000.00) → 201 Created
        mockMvc.perform(post("/api/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1,
                                    "cajeroId": 5,
                                    "metodoPago": "EFECTIVO",
                                    "monto": 35000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", equalTo("F-0001")))
                .andExpect(jsonPath("$.subtotal", equalTo(30000.0)))
                .andExpect(jsonPath("$.impuestos", equalTo(2400.0)))
                .andExpect(jsonPath("$.total", equalTo(32400.0)))
                .andExpect(jsonPath("$.anulada", equalTo(false)));

        // Verificar que el pedido quedó en estado PAGADO
        mockMvc.perform(get("/api/pedidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", equalTo("PAGADO")));

        // 7. GET /api/mesas/6 confirma que la mesa volvió a estado LIBRE automáticamente
        mockMvc.perform(get("/api/mesas/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", equalTo("LIBRE")));

        // 8. DELETE /api/facturas/1 anula la factura y devuelve 200 OK con anulada = true
        mockMvc.perform(delete("/api/facturas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulada", equalTo(true)));

        // Intentar anularla nuevamente → 409 Conflict (ya está anulada)
        mockMvc.perform(delete("/api/facturas/1"))
                .andExpect(status().isConflict());
    }
}
