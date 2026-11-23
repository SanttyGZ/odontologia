package dev.odontologia.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import dev.odontologia.OdontologiaApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

class LocalEnvironmentPostProcessorTest {

    private final LocalEnvironmentPostProcessor processor = new LocalEnvironmentPostProcessor();

    @Test
    void acceptsExplicitConfigurationWithoutConnectingToDatabase() {
        assertThatCode(() -> validate(validProperties())).doesNotThrowAnyException();
    }

    static Stream<String> requiredProperties() {
        return validProperties().keySet().stream();
    }

    @ParameterizedTest
    @MethodSource("requiredProperties")
    void rejectsEachMissingPropertyWithoutExposingOtherValues(String property) {
        Map<String, String> properties = validProperties();
        properties.remove(property);
        Throwable failure = catchThrowable(() -> validate(properties));
        assertThat(failure).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configuración local inválida")
                .hasNoCause();
        assertThat(failure.getMessage()).doesNotContain("fixture-secret", "fixture-user");
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.consultorio-nombre", "app.odontologo-nombre"})
    void rejectsBlankInitializationNames(String property) {
        Map<String, String> properties = validProperties();
        properties.put(property, " \t");
        assertThat(catchThrowable(() -> validate(properties)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(property.toUpperCase(java.util.Locale.ROOT).replace('.', '_').replace('-', '_'));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Mars/Base", "+03:00", "UTC+3"})
    void rejectsAbsentOrNonIanaTimeZones(String zone) {
        Map<String, String> properties = validProperties();
        properties.put("app.time-zone", zone);
        assertThat(catchThrowable(() -> validate(properties)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_TIME_ZONE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"relative", "file:///tmp/test", "http://user:fixture-secret@localhost",
            "http://localhost/?secret=fixture-secret", "http://localhost/#fixture-secret", "http://[fixture-secret"})
    void rejectsMalformedOrCredentialBearingBaseUrlsWithoutEchoingThem(String url) {
        Map<String, String> properties = validProperties();
        properties.put("app.base-url", url);
        Throwable failure = catchThrowable(() -> validate(properties));
        assertThat(failure).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_BASE_URL").hasNoCause();
        assertThat(failure.getMessage()).doesNotContain("fixture-secret");
    }

    @Test
    void rejectsNonPostgresDatabase() {
        Map<String, String> properties = validProperties();
        properties.put("spring.datasource.url", "jdbc:h2:mem:fixture");
        assertThat(catchThrowable(() -> validate(properties)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SPRING_DATASOURCE_URL");
    }

    @Test
    void restrictsHttpBindingAndBrowserOriginToLoopback() {
        Map<String, String> properties = validProperties();
        properties.put("server.address", "0.0.0.0");
        assertThat(catchThrowable(() -> validate(properties)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SERVER_ADDRESS");

        properties.put("server.address", "127.0.0.1");
        properties.put("app.base-url", "http://192.0.2.1:5173");
        assertThat(catchThrowable(() -> validate(properties)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_BASE_URL");

        properties.put("app.base-url", "http://[::1]:5173");
        assertThatCode(() -> validate(properties)).doesNotThrowAnyException();
    }

    private void validate(Map<String, String> properties) {
        MockEnvironment environment = new MockEnvironment();
        properties.forEach(environment::setProperty);
        processor.postProcessEnvironment(environment, new SpringApplication(OdontologiaApplication.class));
    }

    private static Map<String, String> validProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("app.base-url", "http://127.0.0.1:5173");
        properties.put("app.time-zone", "Etc/UTC");
        properties.put("app.consultorio-nombre", "Consultorio de prueba");
        properties.put("app.odontologo-nombre", "Profesional de prueba");
        properties.put("spring.datasource.url", "jdbc:postgresql://127.0.0.1:1/unused");
        properties.put("spring.datasource.username", "fixture-user");
        properties.put("spring.datasource.password", "fixture-secret");
        return properties;
    }
}
