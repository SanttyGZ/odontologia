package dev.odontologia.acceso;

import java.io.Console;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm;

public final class DentistCredentialFile {

    private static final String FORMAT = "pbkdf2-sha256-v1";
    private static final int MAX_FILE_BYTES = 512;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9._-]{1,64}");
    private static final Pattern VERIFIER = Pattern.compile("[a-f0-9]{96}");
    private static final PasswordEncoder PASSWORDS = new Pbkdf2PasswordEncoder("", 16, 600_000,
            SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);

    private DentistCredentialFile() {
    }

    static Optional<Credential> read(Path file) {
        try {
            Path location = privateLocation(file);
            var attributes = Files.readAttributes(location, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            String permissions = PosixFilePermissions.toString(attributes.permissions());
            if (!attributes.isRegularFile() || !attributes.owner().equals(Files.getOwner(location.getParent()))
                    || !(permissions.equals("rw-------") || permissions.equals("r--------"))) {
                return Optional.empty();
            }

            var bytes = ByteBuffer.allocate(MAX_FILE_BYTES);
            try (var channel = Files.newByteChannel(location, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                while (bytes.hasRemaining() && channel.read(bytes) != -1) {
                    // Un archivo regular puede requerir más de una lectura.
                }
            }
            if (!bytes.hasRemaining()) {
                return Optional.empty();
            }
            String[] lines = StandardCharsets.UTF_8.decode(bytes.flip()).toString().lines().toArray(String[]::new);
            if (lines.length != 3 || !FORMAT.equals(lines[0]) || !USERNAME.matcher(lines[1]).matches()
                    || !VERIFIER.matcher(lines[2]).matches()) {
                return Optional.empty();
            }
            return Optional.of(new Credential(lines[1], lines[2]));
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            return Optional.empty();
        }
    }

    public static void write(Path file, String username, char[] password) throws IOException {
        if (!USERNAME.matcher(username).matches() || password.length < 15 || password.length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Usar un usuario de 1 a 64 caracteres (letras, números, punto, guion o guion bajo) y una contraseña de 15 a 128 caracteres.");
        }
        Path location = privateLocation(file);
        if (Files.exists(location, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(location, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("El destino debe ser un archivo regular.");
        }
        String verifier = PASSWORDS.encode(CharBuffer.wrap(password));
        var permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
        Path temporary = Files.createTempFile(location.getParent(), ".acceso-", ".tmp", permissions);
        try {
            Files.writeString(temporary, FORMAT + "\n" + username + "\n" + verifier + "\n");
            Files.move(temporary, location, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void promptAndWrite(Path file, Console console) throws IOException {
        if (console == null) {
            throw new IOException("La creación de credenciales requiere una terminal interactiva.");
        }
        String username = console.readLine("Usuario del odontólogo: ");
        char[] password = console.readPassword("Contraseña: ");
        char[] confirmation = null;
        try {
            confirmation = console.readPassword("Repetir contraseña: ");
            if (username == null || password == null || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("La contraseña y la confirmación deben coincidir.");
            }
            write(file, username, password);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }

    private static Path privateLocation(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        if (absolute.getParent() == null) {
            throw new IOException("Indicar un archivo dentro de un directorio privado existente.");
        }
        Path directory = absolute.getParent().toRealPath();
        String permissions = PosixFilePermissions.toString(Files.getPosixFilePermissions(directory));
        if (!permissions.equals("rwx------")) {
            throw new IOException("El directorio de la credencial debe tener permisos 0700.");
        }
        for (Path ancestor = directory; ancestor != null; ancestor = ancestor.getParent()) {
            if (Files.exists(ancestor.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Guardar la credencial fuera del repositorio.");
            }
        }
        return directory.resolve(absolute.getFileName());
    }

    static final class Credential {
        private final String username;
        private final String verifier;

        private Credential(String username, String verifier) {
            this.username = username;
            this.verifier = verifier;
        }

        boolean matches(String candidate, String password) {
            return password.length() <= MAX_PASSWORD_LENGTH && PASSWORDS.matches(password, verifier) && username.equals(candidate);
        }
    }
}
