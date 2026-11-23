package dev.odontologia.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/** Valida antes de crear beans, sin incorporar valores de conexión a los errores. */
public final class LocalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String baseUrl = required(environment, "app.base-url", "APP_BASE_URL");
        URI browserOrigin = validateBaseUrl(baseUrl);
        boolean ssl = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        if (!ssl) {
            String address = environment.getProperty("server.address", "127.0.0.1");
            if (!isLoopback(address)) {
                throw invalid("SERVER_ADDRESS debe ser una dirección de loopback para HTTP.");
            }
        }
        if ("http".equals(browserOrigin.getScheme()) && !isLoopback(browserOrigin.getHost())) {
            throw invalid("APP_BASE_URL debe usar un origen de loopback para HTTP.");
        }

        String timeZone = required(environment, "app.time-zone", "APP_TIME_ZONE");
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw invalid("APP_TIME_ZONE debe ser un identificador IANA válido.");
        }
        required(environment, "app.consultorio-nombre", "APP_CONSULTORIO_NOMBRE");
        required(environment, "app.odontologo-nombre", "APP_ODONTOLOGO_NOMBRE");

        String databaseUrl = required(environment, "spring.datasource.url", "SPRING_DATASOURCE_URL");
        if (!databaseUrl.startsWith("jdbc:postgresql://")) {
            throw invalid("SPRING_DATASOURCE_URL debe indicar PostgreSQL mediante JDBC.");
        }
        required(environment, "spring.datasource.username", "SPRING_DATASOURCE_USERNAME");
        required(environment, "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");
    }

    private static String required(ConfigurableEnvironment environment, String property, String variable) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw invalid("Falta configurar " + variable + ".");
        }
        return value;
    }

    private static URI validateBaseUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw invalid("APP_BASE_URL debe ser una URL HTTP(S) absoluta sin credenciales, query ni fragmento.");
            }
            return uri;
        } catch (URISyntaxException exception) {
            // No conservar la excepción original: contiene el valor recibido.
            throw invalid("APP_BASE_URL debe ser una URL HTTP(S) absoluta válida.");
        }
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host);
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("Configuración local inválida: " + message);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
