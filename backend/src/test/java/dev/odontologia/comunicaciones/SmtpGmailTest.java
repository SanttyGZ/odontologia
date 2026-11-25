package dev.odontologia.comunicaciones;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import jakarta.mail.AuthenticationFailedException;
import org.eclipse.angus.mail.util.MailConnectException;
import org.eclipse.angus.mail.util.SocketConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import static dev.odontologia.comunicaciones.AutorizacionDespacho.Resultado.*;
import static org.assertj.core.api.Assertions.assertThat;

class SmtpGmailTest {
    @Test
    void reintentaSoloSiSeSabeQueLaConexionNoSeEstablecio() {
        var conexion = new MailConnectException(
                new SocketConnectException("prueba", new ConnectException(), "localhost", 587, 5000));
        assertThat(SmtpGmail.clasificar(new MailSendException("prueba", conexion))).isEqualTo(TRANSITORIO);
        assertThat(SmtpGmail.clasificar(new MailSendException("prueba", new SocketTimeoutException())))
                .isEqualTo(INCIERTO);
    }

    @Test
    void distingueAuthTemporalDeRechazoPermanente() {
        assertThat(SmtpGmail.clasificar(new MailAuthenticationException(
                new AuthenticationFailedException("454 4.7.0 Temporary authentication failure")))).isEqualTo(TRANSITORIO);
        assertThat(SmtpGmail.clasificar(new MailAuthenticationException(
                new AuthenticationFailedException("535 5.7.8 Authentication rejected")))).isEqualTo(PERMANENTE);
        assertThat(SmtpGmail.clasificar(new MailAuthenticationException(
                new AuthenticationFailedException("Rejected; reference 454")))).isEqualTo(PERMANENTE);
    }

    @Test
    void distingueRechazosConocidosYResultadosInciertos() {
        assertThat(SmtpGmail.clasificarCodigo(421)).isEqualTo(TRANSITORIO);
        assertThat(SmtpGmail.clasificarCodigo(550)).isEqualTo(PERMANENTE);
        assertThat(SmtpGmail.clasificarCodigo(-1)).isEqualTo(INCIERTO);
    }

    @Test
    void bloqueaOtroDestinatarioAntesDeUsarLaRed() {
        var correo = new SmtpGmail("secreto-ficticio");
        assertThat(correo.enviar("otro@example.test", new ComposicionAvisos.Aviso("prueba", "prueba")))
                .isEqualTo(PERMANENTE);
    }
}
