package com.restaurante.usuarios;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Par de claves RSA para las pruebas. Se genera una sola vez en memoria y se escribe en una
 * carpeta temporal, de modo que las pruebas no dependen de la carpeta claves/ del repositorio.
 */
final class ClavesRsaTest {
    private static final KeyPair PAR = generarPar();
    private static final Path CARPETA = escribirPem(PAR);

    private ClavesRsaTest() {
    }

    /** Se llama desde el método @DynamicPropertySource de cada clase @SpringBootTest. */
    static void registrar(DynamicPropertyRegistry registro) {
        registro.add("seguridad.jwt.clave-privada", () -> CARPETA.resolve("privada.pem").toUri().toString());
        registro.add("seguridad.jwt.clave-publica", () -> CARPETA.resolve("publica.pem").toUri().toString());
    }

    /** La clave pública del par con el que firma el servicio durante las pruebas. */
    static RSAPublicKey clavePublica() {
        return (RSAPublicKey) PAR.getPublic();
    }

    /** La clave pública de un par distinto, para comprobar que una firma ajena se rechaza. */
    static RSAPublicKey otraClavePublica() {
        return (RSAPublicKey) generarPar().getPublic();
    }

    private static KeyPair generarPar() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el par de claves RSA de prueba.", e);
        }
    }

    private static Path escribirPem(KeyPair par) {
        try {
            Path carpeta = Files.createTempDirectory("claves-prueba-");
            Path privada = carpeta.resolve("privada.pem");
            Path publica = carpeta.resolve("publica.pem");
            Files.writeString(privada, aPem("PRIVATE KEY", par.getPrivate().getEncoded()));
            Files.writeString(publica, aPem("PUBLIC KEY", par.getPublic().getEncoded()));
            // Se borran en orden inverso al registro: primero los archivos y al final la carpeta
            carpeta.toFile().deleteOnExit();
            privada.toFile().deleteOnExit();
            publica.toFile().deleteOnExit();
            return carpeta;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron escribir los PEM de prueba.", e);
        }
    }

    private static String aPem(String tipo, byte[] bytes) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(bytes);
        return "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----\n";
    }
}
