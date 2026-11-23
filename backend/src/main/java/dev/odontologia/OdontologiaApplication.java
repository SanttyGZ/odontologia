package dev.odontologia;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import dev.odontologia.acceso.DentistCredentialFile;
import dev.odontologia.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class OdontologiaApplication {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("credencial-odontologo")) {
            writeDentistCredential(args);
            return;
        }
        SpringApplication.run(OdontologiaApplication.class, args);
    }

    private static void writeDentistCredential(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java -jar APLICACION.jar credencial-odontologo ARCHIVO");
            System.exit(2);
        }
        try {
            DentistCredentialFile.promptAndWrite(Path.of(args[1]), System.console());
            System.out.println("Credencial del odontólogo guardada.");
        } catch (IllegalArgumentException | IOException | UnsupportedOperationException | SecurityException exception) {
            System.err.println("No se pudo guardar la credencial. Revisar la ruta, los permisos y la confirmación de la contraseña.");
            System.exit(1);
        }
    }
}
