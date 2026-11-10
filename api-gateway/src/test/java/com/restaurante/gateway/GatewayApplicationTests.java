package com.restaurante.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class GatewayApplicationTests {

	@DynamicPropertySource
	static void propiedades(DynamicPropertyRegistry registro) {
		ClavesDePrueba.registrar(registro);
	}

	@Test
	void contextLoads() {
	}

}
