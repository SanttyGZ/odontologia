package dev.odontologia.acceso;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DentistCredentialFileTest {

    @TempDir
    Path directory;

    @Test
    void writesPrivateSaltedVerifierAndReplacesTheCredential() throws Exception {
        Path file = directory.resolve("access");
        char[] original = "primera clave de prueba".toCharArray();
        char[] replacement = "segunda clave de prueba".toCharArray();

        DentistCredentialFile.write(file, "profesional", original);
        String first = Files.readString(file);
        assertThat(first).doesNotContain(new String(original));
        assertThat(Files.getPosixFilePermissions(file)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(DentistCredentialFile.read(file).orElseThrow().matches("profesional", new String(original))).isTrue();

        DentistCredentialFile.write(file, "profesional", original);
        assertThat(Files.readString(file)).isNotEqualTo(first);

        DentistCredentialFile.write(file, "nuevo", replacement);
        var credential = DentistCredentialFile.read(file).orElseThrow();
        assertThat(credential.matches("profesional", new String(original))).isFalse();
        assertThat(credential.matches("nuevo", new String(original))).isFalse();
        assertThat(credential.matches("nuevo", new String(replacement))).isTrue();
    }

    @Test
    void rejectsMissingMalformedAndAccessibleFiles() throws Exception {
        Path file = directory.resolve("access");
        assertThat(DentistCredentialFile.read(file)).isEmpty();
        var permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
        Files.createFile(file, permissions);
        Files.writeString(file, "formato inválido\n");
        assertThat(DentistCredentialFile.read(file)).isEmpty();

        DentistCredentialFile.write(file, "profesional", "una clave de prueba".toCharArray());
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        assertThat(DentistCredentialFile.read(file)).isEmpty();
    }

    @Test
    void rejectsLinksAndFilesInsideAGitCheckout() throws Exception {
        Path file = directory.resolve("access");
        char[] password = "una clave de prueba".toCharArray();
        DentistCredentialFile.write(file, "profesional", password);
        Path link = Files.createSymbolicLink(directory.resolve("link"), file);
        assertThat(DentistCredentialFile.read(link)).isEmpty();
        assertThatThrownBy(() -> DentistCredentialFile.write(link, "profesional", password))
                .isInstanceOf(java.io.IOException.class);
        assertThat(DentistCredentialFile.read(file)).isPresent();

        Path checkout = Files.createDirectory(directory.resolve("checkout"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Files.createDirectory(checkout.resolve(".git"));
        assertThatThrownBy(() -> DentistCredentialFile.write(checkout.resolve("access"), "profesional", password))
                .isInstanceOf(java.io.IOException.class).hasMessageContaining("fuera del repositorio");
    }
}
