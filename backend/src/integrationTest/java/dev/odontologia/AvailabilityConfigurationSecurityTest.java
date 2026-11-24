package dev.odontologia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import dev.odontologia.acceso.DentistCredentialFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityConfigurationSecurityTest {

    private static final String USERNAME = "odontologo-c06-fixture";
    private static final String PASSWORD = "credencial de prueba para C-06";

    @TempDir
    Path directory;

    @Test
    void availabilityRoutesKeepTheInternalSessionAndCsrfRequirements() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            Path credential = directory.resolve("access");
            DentistCredentialFile.write(credential, USERNAME, PASSWORD.toCharArray());
            try (var app = start(db, credential)) {
                var browser = new Browser("http://127.0.0.1:" + app.getWebServer().getPort());
                var read = browser.send("GET", "/api/odontologo/motivos", null);
                assertThat(read.statusCode()).isEqualTo(401);
                assertThat(read.body()).isEqualTo("{\"codigo\":\"SESSION_REQUIRED\"}");

                var write = browser.send("POST", "/api/odontologo/bloqueos",
                        "{\"inicio\":\"2030-01-07T12:00:00Z\",\"fin\":\"2030-01-07T13:00:00Z\"}");
                assertThat(write.statusCode()).isEqualTo(403);
                assertThat(write.body()).isEqualTo("{\"codigo\":\"CSRF_INVALID\"}");
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM bloqueo_agenda WHERE agenda_id = ?",
                        Long.class, fixture.agenda())).isZero();
            }
        }
    }

    private static ServletWebServerApplicationContext start(TestDatabase db, Path credential) {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(OdontologiaApplication.class).run(
                "--spring.datasource.url=" + db.url(), "--server.port=0", "--server.address=127.0.0.1",
                "--app.base-url=http://127.0.0.1:5173", "--app.time-zone=Etc/UTC",
                "--app.consultorio-nombre=" + CoreSchemaTest.TEST_CONSULTORIO_NOMBRE,
                "--app.odontologo-nombre=" + CoreSchemaTest.TEST_ODONTOLOGO_NOMBRE,
                "--app.dentist-credential-file=" + credential, "--spring.main.banner-mode=off");
    }

    private static final class Browser {
        private final String base;
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        private Browser(String base) {
            this.base = base;
        }

        private HttpResponse<String> send(String method, String path, String json) throws Exception {
            var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5));
            if (json != null) {
                request.header("Content-Type", "application/json");
            }
            var body = json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json);
            return client.send(request.method(method, body).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
