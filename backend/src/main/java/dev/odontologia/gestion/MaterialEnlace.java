package dev.odontologia.gestion;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class MaterialEnlace {
    record Cifrado(byte[] verificador, byte[] contenido, byte[] nonce, String version) {}

    private final SecureRandom random = new SecureRandom();
    private final String archivo;

    MaterialEnlace(@Value("${app.gestion-key-file:}") String archivo) {
        this.archivo = archivo;
    }

    Cifrado emitir(UUID id, UUID turno) {
        Properties claves = claves();
        String version = claves.getProperty("activa");
        if (version == null || version.isBlank()) {
            throw noDisponible();
        }
        byte[] secreto = new byte[32];
        byte[] nonce = new byte[12];
        random.nextBytes(secreto);
        random.nextBytes(nonce);
        return new Cifrado(verificador(secreto),
                transformar(Cipher.ENCRYPT_MODE, claves, version, nonce, id, turno, secreto), nonce, version);
    }

    String recuperar(UUID id, UUID turno, Cifrado cifrado) {
        byte[] secreto = transformar(Cipher.DECRYPT_MODE, claves(), cifrado.version(), cifrado.nonce(),
                id, turno, cifrado.contenido());
        if (!MessageDigest.isEqual(verificador(secreto), cifrado.verificador())) {
            throw noDisponible();
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
    }

    static byte[] verificador(byte[] secreto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secreto);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("SHA-256 no disponible", failure);
        }
    }

    private Properties claves() {
        if (archivo.isBlank()) {
            throw noDisponible();
        }
        Properties propiedades = new Properties();
        try {
            Path ruta = Path.of(archivo);
            Path absoluta = ruta.toAbsolutePath().normalize();
            Path directorio = absoluta.getParent().toRealPath();
            for (Path padre = directorio; padre != null; padre = padre.getParent()) {
                if (Files.exists(padre.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                    throw noDisponible();
                }
            }
            try (var stream = Files.newDirectoryStream(directorio)) {
                if (!(stream instanceof SecureDirectoryStream<Path> seguro)) {
                    throw noDisponible();
                }
                var atributosDir = seguro.getFileAttributeView(PosixFileAttributeView.class).readAttributes();
                var propietario = ruta.getFileSystem().getUserPrincipalLookupService()
                        .lookupPrincipalByName(System.getProperty("user.name"));
                if (!atributosDir.owner().equals(propietario)
                        || !PosixFilePermissions.toString(atributosDir.permissions()).matches("r[w-]x------")) {
                    throw noDisponible();
                }
                Path nombre = absoluta.getFileName();
                PosixFileAttributes atributos = seguro.getFileAttributeView(nombre, PosixFileAttributeView.class,
                        LinkOption.NOFOLLOW_LINKS).readAttributes();
                String permisos = PosixFilePermissions.toString(atributos.permissions());
                if (!atributos.isRegularFile() || !atributos.owner().equals(propietario)
                        || !(permisos.equals("rw-------") || permisos.equals("r--------"))) {
                    throw noDisponible();
                }
                try (var canal = seguro.newByteChannel(nombre, Set.of(StandardOpenOption.READ,
                        LinkOption.NOFOLLOW_LINKS)); Reader reader = Channels.newReader(canal, StandardCharsets.UTF_8)) {
                    propiedades.load(reader);
                    return propiedades;
                }
            }
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException failure) {
            throw noDisponible();
        }
    }

    private static byte[] transformar(int modo, Properties claves, String version, byte[] nonce,
            UUID id, UUID turno, byte[] datos) {
        try {
            String valor = claves.getProperty("clave." + version);
            if (valor == null) {
                throw noDisponible();
            }
            byte[] clave = Base64.getDecoder().decode(valor);
            if (clave.length != 32) {
                throw noDisponible();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(modo, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(ByteBuffer.allocate(32)
                    .putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits())
                    .putLong(turno.getMostSignificantBits()).putLong(turno.getLeastSignificantBits()).array());
            return cipher.doFinal(datos);
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            throw noDisponible();
        }
    }

    private static IllegalStateException noDisponible() {
        return new IllegalStateException("Material de enlace no disponible");
    }
}
