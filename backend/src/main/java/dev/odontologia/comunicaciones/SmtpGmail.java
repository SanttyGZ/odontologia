package dev.odontologia.comunicaciones;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import static dev.odontologia.comunicaciones.AutorizacionDespacho.Resultado.*;

final class SmtpGmail implements EmisionAviso.Transporte {
    private final JavaMailSenderImpl correo;

    SmtpGmail(String clave) {
        if (clave.isBlank()) {
            throw new IllegalArgumentException("Falta SPRING_MAIL_PASSWORD para habilitar C-11.");
        }
        correo = new JavaMailSenderImpl();
        correo.setHost(GmailPruebas.HOST);
        correo.setPort(GmailPruebas.PUERTO);
        correo.setUsername(GmailPruebas.BUZON);
        correo.setPassword(clave);
        var propiedades = GmailPruebas.propiedades();
        propiedades.setProperty("mail.smtp.quitwait", "false");
        correo.setJavaMailProperties(propiedades);
    }

    @Override
    public AutorizacionDespacho.Resultado enviar(String destinatario, ComposicionAvisos.Aviso aviso) {
        if (!GmailPruebas.permite(destinatario)) {
            return PERMANENTE;
        }
        var mensaje = correo.createMimeMessage();
        try {
            var formato = new MimeMessageHelper(mensaje, StandardCharsets.UTF_8.name());
            formato.setFrom(GmailPruebas.BUZON, GmailPruebas.NOMBRE);
            formato.setTo(GmailPruebas.BUZON);
            formato.setSubject(aviso.asunto());
            formato.setText(aviso.texto());
        } catch (MessagingException | UnsupportedEncodingException fallo) {
            return PERMANENTE;
        }
        try {
            correo.send(mensaje);
            return ACEPTADA_SMTP;
        } catch (MailException fallo) {
            return clasificar(fallo);
        }
    }

    static AutorizacionDespacho.Resultado clasificarCodigo(int codigo) {
        if (codigo >= 400 && codigo < 500) {
            return TRANSITORIO;
        }
        if (codigo >= 500 && codigo < 600) {
            return PERMANENTE;
        }
        return INCIERTO;
    }

    static AutorizacionDespacho.Resultado clasificar(MailException fallo) {
        if (fallo instanceof MailAuthenticationException) {
            // Angus conserva la respuesta SMTP en la excepción de autenticación.
            if (fallo.getCause() instanceof AuthenticationFailedException autenticacion
                    && autenticacion.getMessage() != null) {
                String respuesta = autenticacion.getMessage().strip();
                if (respuesta.equals("454") || respuesta.startsWith("454 ") || respuesta.startsWith("454-")) {
                    return TRANSITORIO;
                }
            }
            return PERMANENTE;
        }
        var pendientes = new ArrayDeque<Throwable>();
        var revisados = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        pendientes.add(fallo);
        var resultado = INCIERTO;
        while (!pendientes.isEmpty()) {
            Throwable actual = pendientes.removeFirst();
            if (!revisados.add(actual)) {
                continue;
            }
            if (actual instanceof SendFailedException envio && envio.getValidSentAddresses() != null
                    && envio.getValidSentAddresses().length > 0) {
                return INCIERTO;
            }
            int codigo = switch (actual) {
                case SMTPSendFailedException smtp -> smtp.getReturnCode();
                case SMTPSenderFailedException smtp -> smtp.getReturnCode();
                case SMTPAddressFailedException smtp -> smtp.getReturnCode();
                default -> -1;
            };
            var conocido = actual instanceof MailConnectException ? TRANSITORIO : clasificarCodigo(codigo);
            if (conocido == PERMANENTE || (conocido == TRANSITORIO && resultado == INCIERTO)) {
                resultado = conocido;
            }
            if (actual.getCause() != null) {
                pendientes.add(actual.getCause());
            }
            if (actual instanceof MessagingException mensaje && mensaje.getNextException() != null) {
                pendientes.add(mensaje.getNextException());
            }
            if (actual instanceof MailSendException envio) {
                Collections.addAll(pendientes, envio.getMessageExceptions());
            }
        }
        return resultado;
    }
}
