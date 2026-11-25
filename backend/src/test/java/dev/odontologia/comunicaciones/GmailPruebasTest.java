package dev.odontologia.comunicaciones;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GmailPruebasTest {
    @Test
    void permiteSoloElBuzonAutorizado() {
        assertThat(GmailPruebas.permite("grupo16desarrollosoftware3k10@gmail.com")).isTrue();
        assertThat(GmailPruebas.permite("otro@example.test")).isFalse();
        assertThat(GmailPruebas.permite("grupo16desarrollosoftware3k10+otro@gmail.com")).isFalse();
    }

    @Test
    void exigeTlsYAcotaLaConexion() {
        assertThat(GmailPruebas.propiedades())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.required", "true")
                .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
                .containsEntry("mail.smtp.connectiontimeout", "5000")
                .containsEntry("mail.smtp.timeout", "10000")
                .containsEntry("mail.smtp.writetimeout", "10000")
                .containsEntry("mail.debug", "false");
    }
}

