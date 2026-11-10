package com.restaurante.usuarios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class UsuariosApplicationTests {
	@DynamicPropertySource
	static void claves(DynamicPropertyRegistry registro) {
		ClavesRsaTest.registrar(registro);
	}

	@Test
	void contextLoads() {
	}

}