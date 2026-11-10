package com.restaurante.usuarios.config;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class LectorClavesPEM {
    private LectorClavesPEM() {
    }

    public static PrivateKey leerClavePrivada(Resource recurso) {
        byte[] bytes = leerBytesPem(recurso);
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("El archivo " + recurso.getDescription() + " no tiene el formato esperado para una clave privada RSA (PKCS#8). Si empieza con BEGIN RSA PRIVATE KEY, regenérela con scripts/generar-claves-rsa.sh.", e);
        }
    }

    public static RSAPublicKey leerClavePublica(Resource recurso) {
        byte[] bytes = leerBytesPem(recurso);
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(bytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("El archivo " + recurso.getDescription() + " no tiene el formato esperado para una clave pública RSA (X.509).", e);
        }
    }


    private static byte[] leerBytesPem(Resource recurso) {
        try(InputStream entrada = recurso.getInputStream()) {
            String contenido = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = contenido
                    .replaceAll("-----[A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo leer o decodificar el archivo PEM " + recurso.getDescription() + ".", e);
        }
    }
}
