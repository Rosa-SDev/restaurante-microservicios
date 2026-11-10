package com.restaurante.gateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Par de claves RSA exclusivo de las pruebas del gateway.
 *
 * Se genera en memoria una sola vez por ejecución y la clave pública se escribe
 * como PEM en una carpeta temporal, porque el gateway la lee de un archivo. Así
 * las pruebas no dependen de la carpeta claves/ del repositorio, que cada quien
 * genera en su equipo y que no existe en un clon recién descargado.
 */
final class ClavesDePrueba {

    static final KeyPair PAR = generarPar();

    private static final Path PUBLICA = escribirPublica(PAR);

    private ClavesDePrueba() {
    }

    static RSAPublicKey publica() {
        return (RSAPublicKey) PAR.getPublic();
    }

    static RSAPrivateKey privada() {
        return (RSAPrivateKey) PAR.getPrivate();
    }

    /**
     * Apunta las propiedades del gateway a lo que generan las pruebas. La ruta va
     * como URI (file:///...) para que los espacios y las letras de unidad de
     * Windows no causen problemas.
     *
     * El JWKS apunta a una dirección donde no escucha nadie: ninguna prueba debe
     * necesitar Keycloak, y si alguna intentara descargarlo, fallaría a la vista.
     */
    static void registrar(DynamicPropertyRegistry registro) {
        registro.add("seguridad.jwt.clave-publica", () -> PUBLICA.toUri().toString());
        registro.add("seguridad.oauth2.jwks", () -> "http://localhost:9/jwks-inexistente");
    }

    static KeyPair generarPar() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no soporta RSA.", e);
        }
    }

    /**
     * getEncoded() de una clave pública RSA ya devuelve el formato X.509
     * SubjectPublicKeyInfo, el mismo que produce openssl pkey -pubout.
     */
    private static Path escribirPublica(KeyPair par) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(par.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n";
        try {
            Path carpeta = Files.createTempDirectory("gateway-claves-");
            Path archivo = carpeta.resolve("publica.pem");
            Files.writeString(archivo, pem, StandardCharsets.US_ASCII);
            archivo.toFile().deleteOnExit();
            carpeta.toFile().deleteOnExit();
            return archivo;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir la clave pública de prueba.", e);
        }
    }
}
