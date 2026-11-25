package dev.odontologia.comunicaciones;

import java.util.Properties;

final class GmailPruebas {
    static final String BUZON = "grupo16desarrollosoftware3k10@gmail.com";
    static final String NOMBRE = "Odontología — pruebas C-11";
    static final String HOST = "smtp.gmail.com";
    static final int PUERTO = 587;

    private GmailPruebas() {}

    static boolean permite(String destinatario) {
        return BUZON.equalsIgnoreCase(destinatario);
    }

    static Properties propiedades() {
        Properties propiedades = new Properties();
        propiedades.setProperty("mail.smtp.auth", "true");
        propiedades.setProperty("mail.smtp.starttls.enable", "true");
        propiedades.setProperty("mail.smtp.starttls.required", "true");
        propiedades.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        propiedades.setProperty("mail.smtp.connectiontimeout", "5000");
        propiedades.setProperty("mail.smtp.timeout", "10000");
        propiedades.setProperty("mail.smtp.writetimeout", "10000");
        propiedades.setProperty("mail.debug", "false");
        return propiedades;
    }
}
