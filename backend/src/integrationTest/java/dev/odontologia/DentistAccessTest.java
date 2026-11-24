package dev.odontologia;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import dev.odontologia.acceso.DentistCredentialFile;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.gestion.CredencialesGestion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DentistAccessTest {

    private static final String ROOT = "/api/odontologo";
    private static final String SESSION = ROOT + "/sesion";
    private static final String LOGOUT = SESSION + "/cierre";
    private static final String FIXTURE = ROOT + "/control-prueba";
    private static final String USERNAME = "odontologo-fixture";
    private static final String PASSWORD = "credencial larga para la prueba";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path directory;

    @Test
    void recuperacionExigeSesionYCsrfYRevierteTodoAnteFallo() throws Exception {
        Path keys = Files.createTempFile(directory, "gestion-", ".properties");
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        Files.writeString(keys, "activa=fixture\nclave.fixture=" + Base64.getEncoder().encodeToString(key));
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = start(db, credential(), keys)) {
            var inicio = CoreSchemaTest.START.plusYears(10);
            UUID turno = UUID.randomUUID();
            CoreSchemaTest.insertTurn(db.jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
            var credenciales = app.getBean(CredencialesGestion.class);
            UUID anterior = app.getBean(AgendaWriteCoordinator.class).write(fixture.agenda(),
                    () -> credenciales.emitir(turno));
            String secreto = credenciales.recuperarParaComposicion(anterior, turno, 0).orElseThrow();
            var path = ROOT + "/turnos/" + turno + "/recuperacion-enlace";
            String request = """
                    {"motivo":"Enlace perdido","metodo":"DOCUMENTO_PRESENCIAL",
                     "verificacionConfirmada":true,"emailConfirmado":"Paciente.Anterior@example.test"}
                    """;
            var browser = browser(app);
            assertError(browser.json(path, null, request), 403, "CSRF_INVALID");
            assertError(browser.json(path, browser.csrf(), request), 401, "SESSION_REQUIRED");
            assertThat(browser.login(USERNAME, PASSWORD, browser.csrf()).statusCode()).isEqualTo(204);
            assertError(browser.json(path, null, request), 403, "CSRF_INVALID");
            assertThat(browser.json(path, browser.csrf(), request.replace(":true", ":false")).statusCode())
                    .isEqualTo(400);
            assertThat(credenciales.validar(secreto)).isPresent();

            db.jdbc.execute("ALTER TABLE intencion_notificacion ADD CONSTRAINT fallo_fixture CHECK (NOT recuperacion)");
            assertThat(browser.json(path, browser.csrf(), request).statusCode()).isEqualTo(500);
            assertThat(credenciales.validar(secreto)).isPresent();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM credencial_gestion WHERE turno_id = ?",
                    Long.class, turno)).isEqualTo(1L);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM recuperacion_enlace", Long.class)).isZero();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class)).isZero();
            db.jdbc.execute("ALTER TABLE intencion_notificacion DROP CONSTRAINT fallo_fixture");

            UUID recordatorio = UUID.randomUUID();
            db.jdbc.update("""
                    INSERT INTO intencion_notificacion
                        (id, evento_id, turno_id, revision, tipo, destinatario, credencial_id,
                         programada_en, vence_en, despacho_id)
                    VALUES (?, ?, ?, 0, 'RECORDATORIO', 'Paciente.Anterior@example.test', ?, ?, ?, ?)
                    """, recordatorio, UUID.randomUUID(), turno, anterior, inicio.minusDays(1),
                    inicio.minusHours(22), recordatorio);
            db.jdbc.update("UPDATE paciente SET email = 'edicion@example.test' WHERE id = ?", fixture.paciente());
            assertThat(browser.json(path, browser.csrf(),
                    request.replace("\"Paciente.Anterior@example.test\"", "null")).statusCode()).isEqualTo(400);
            assertThat(credenciales.validar(secreto)).isPresent();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM recuperacion_enlace", Long.class)).isZero();

            var response = browser.json(path, browser.csrf(), request);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
            assertThat(response.body()).doesNotContain(secreto, "Paciente.Anterior@example.test");
            var referencia = UUID.fromString(JSON.readTree(response.body()).path("referencia").asString());
            assertThat(credenciales.validar(secreto)).isEmpty();
            assertThat(db.jdbc.queryForObject("SELECT destinatario FROM intencion_notificacion WHERE evento_id = ?",
                    String.class, referencia)).isEqualTo("Paciente.Anterior@example.test");
            assertThat(db.jdbc.queryForObject("SELECT destinatario FROM recuperacion_enlace WHERE id = ?",
                    String.class, referencia)).isEqualTo("Paciente.Anterior@example.test");
            assertThat(db.jdbc.queryForObject("""
                    SELECT r.elegible AND r.destinatario = n.destinatario AND r.credencial_id = n.credencial_id
                    FROM intencion_notificacion r CROSS JOIN intencion_notificacion n
                    WHERE r.id = ? AND n.evento_id = ?
                    """, Boolean.class, recordatorio, referencia)).isTrue();
            var segunda = browser.json(path, browser.csrf(),
                    request.replace("Paciente.Anterior@example.test", "corregido@example.test"));
            assertThat(segunda.statusCode()).isEqualTo(200);
            var segundoEvento = UUID.fromString(JSON.readTree(segunda.body()).path("referencia").asString());
            assertThat(db.jdbc.queryForObject("SELECT destinatario FROM intencion_notificacion WHERE evento_id = ?",
                    String.class, segundoEvento)).isEqualTo("corregido@example.test");
            assertThat(db.jdbc.queryForObject("SELECT destinatario FROM intencion_notificacion WHERE id = ?",
                    String.class, recordatorio)).isEqualTo("corregido@example.test");
            }
        }
    }

    @Test
    void restrictsTheWholeInternalFamilyAndRotatesTheSessionAndCsrf() throws Exception {
        try (var db = new TestDatabase(); var app = start(db, credential())) {
            var browser = browser(app);
            assertError(browser.get(FIXTURE), 401, "SESSION_REQUIRED");
            assertError(browser.send("POST", FIXTURE, null, null), 403, "CSRF_INVALID");
            assertThat(browser.get("/api/public/control-prueba").body()).isEqualTo("publico");
            assertThat(browser.send("POST", "/api/public/control-prueba", null, null).body()).isEqualTo("publico");
            assertThat(browser.get("/api/gestion/control-prueba").body()).isEqualTo("gestion");
            assertThat(browser.get("/login").statusCode()).isEqualTo(404);

            var preLogin = browser.csrf();
            String anonymousCookie = browser.cookie;
            assertError(browser.send("POST", LOGOUT, preLogin, null), 401, "SESSION_REQUIRED");
            assertError(browser.send("POST", FIXTURE, preLogin, null), 401, "SESSION_REQUIRED");
            assertError(browser.login(USERNAME, PASSWORD, null), 403, "CSRF_INVALID");
            assertError(browser.send("POST", SESSION + "?parametro=1", preLogin, "username=fixture"), 400,
                    "INVALID_LOGIN_REQUEST");
            assertError(browser.send("POST", SESSION, preLogin, null), 400, "INVALID_LOGIN_REQUEST");
            assertError(browser.login("otra-cuenta", PASSWORD, preLogin), 401, "INVALID_CREDENTIALS");
            assertError(browser.login(USERNAME, "incorrecta", preLogin), 401, "INVALID_CREDENTIALS");

            var response = browser.login(USERNAME, PASSWORD, preLogin);
            assertThat(response.statusCode()).isEqualTo(204);
            assertThat(browser.cookie).isNotEqualTo(anonymousCookie);
            String authenticatedCookie = browser.cookie;
            assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("Path=/api/odontologo", "HttpOnly", "SameSite=Strict").doesNotContain("Secure");
            var previousBrowser = browser(app);
            previousBrowser.cookie = anonymousCookie;
            assertError(previousBrowser.get(SESSION), 401, "SESSION_REQUIRED");
            assertThat(browser.get(SESSION).body()).isEqualTo("{\"autenticado\":true}");
            assertThat(browser.get(FIXTURE).body()).isEqualTo("0:1800");
            assertThat(browser.get("/api/public/control-prueba").body()).isEqualTo("publico");
            assertThat(browser.get("/api/gestion/control-prueba").body()).isEqualTo("gestion");

            assertError(browser.send("POST", FIXTURE, preLogin, null), 403, "CSRF_INVALID");
            var authenticatedToken = browser.csrf();
            assertThat(browser.send("POST", FIXTURE, authenticatedToken, null).body()).isEqualTo("1");
            assertError(browser.send("POST", LOGOUT, null, null), 403, "CSRF_INVALID");
            assertThat(browser.get(FIXTURE).body()).isEqualTo("1:1800");
            var closed = browser.send("POST", LOGOUT, authenticatedToken, null);
            assertThat(closed.statusCode()).isEqualTo(204);
            var deletedCookie = HttpCookie.parse(closed.headers().firstValue("Set-Cookie").orElseThrow()).getFirst();
            assertThat(deletedCookie.getPath()).isEqualTo(ROOT);
            assertThat(deletedCookie.getMaxAge()).isZero();
            assertError(browser.get(SESSION), 401, "SESSION_REQUIRED");
            browser.cookie = authenticatedCookie;
            assertError(browser.get(FIXTURE), 401, "SESSION_REQUIRED");
            var newToken = browser.csrf();
            assertError(browser.login(USERNAME, PASSWORD, authenticatedToken), 403, "CSRF_INVALID");
            assertThat(browser.login(USERNAME, PASSWORD, newToken).statusCode()).isEqualTo(204);
        }
    }

    @Test
    void rejectsAnExpiredSession() throws Exception {
        try (var db = new TestDatabase(); var app = start(db, credential())) {
            var browser = browser(app);
            assertThat(browser.login(USERNAME, PASSWORD, browser.csrf()).statusCode()).isEqualTo(204);
            assertThat(browser.send("POST", FIXTURE + "/expirar", browser.csrf(), null).statusCode()).isEqualTo(200);
            // Consultar durante el plazo actualizaría el instante de última actividad.
            Thread.sleep(Duration.ofMillis(1300));
            assertError(browser.get(SESSION), 401, "SESSION_REQUIRED");
        }
    }

    @Test
    void reenablesAccessAfterLocalProvisioningAndPreservesTheAgendaOnRotation() throws Exception {
        try (var db = new TestDatabase()) {
            Path missing = directory.resolve("access");
            String agenda;
            try (var app = start(db, missing)) {
                var browser = browser(app);
                assertError(browser.login(USERNAME, PASSWORD, browser.csrf()), 503, "ACCESS_NOT_CONFIGURED");
                assertError(browser.get(FIXTURE), 401, "SESSION_REQUIRED");
                agenda = db.jdbc.queryForObject("SELECT id::text FROM agenda", String.class);
            }
            String oldCookie;
            try (var app = start(db, credential())) {
                var browser = browser(app);
                assertThat(browser.login(USERNAME, PASSWORD, browser.csrf()).statusCode()).isEqualTo(204);
                oldCookie = browser.cookie;
            }
            try (var app = start(db, missing)) {
                var browser = browser(app);
                browser.cookie = oldCookie;
                assertError(browser.get(SESSION), 401, "SESSION_REQUIRED");
                assertThat(browser.login(USERNAME, PASSWORD, browser.csrf()).statusCode()).isEqualTo(204);
            }
            String replacement = "otra credencial larga de prueba";
            DentistCredentialFile.write(missing, USERNAME, replacement.toCharArray());
            try (var app = start(db, missing)) {
                var browser = browser(app);
                assertError(browser.login(USERNAME, PASSWORD, browser.csrf()), 401, "INVALID_CREDENTIALS");
                assertThat(browser.login(USERNAME, replacement, browser.csrf()).statusCode()).isEqualTo(204);
                assertThat(db.jdbc.queryForObject("SELECT id::text FROM agenda", String.class)).isEqualTo(agenda);
            }
        }
    }

    private Path credential() throws Exception {
        Path path = directory.resolve("access");
        DentistCredentialFile.write(path, USERNAME, PASSWORD.toCharArray());
        return path;
    }

    private static ServletWebServerApplicationContext start(TestDatabase db, Path credential) {
        return start(db, credential, null);
    }

    private static ServletWebServerApplicationContext start(TestDatabase db, Path credential, Path keys) {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(
                OdontologiaApplication.class, HttpFixture.class).run(
                        "--spring.datasource.url=" + db.url(), "--server.port=0", "--server.address=127.0.0.1",
                        "--app.base-url=http://127.0.0.1:5173", "--app.time-zone=Etc/UTC",
                        "--app.consultorio-nombre=" + (keys == null ? "Consultorio de pruebas" : "Demo"),
                        "--app.odontologo-nombre=" + (keys == null ? "Odontólogo ficticio" : "Profesional de prueba"),
                        "--app.dentist-credential-file=" + credential, "--spring.main.banner-mode=off",
                        "--app.gestion-key-file=" + (keys == null ? "" : keys));
    }

    private static Browser browser(ServletWebServerApplicationContext app) {
        return new Browser("http://127.0.0.1:" + app.getWebServer().getPort());
    }

    private static void assertError(HttpResponse<String> response, int status, String error) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(JSON.readTree(response.body()).path("codigo").asString()).isEqualTo(error);
    }

    record Token(String header, String value) {
    }

    private static final class Browser {
        private final String base;
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        private String cookie;

        private Browser(String base) {
            this.base = base;
        }

        private Token csrf() throws Exception {
            var response = get(ROOT + "/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
            var body = JSON.readTree(response.body());
            return new Token(body.path("headerName").asString(), body.path("token").asString());
        }

        private HttpResponse<String> login(String username, String password, Token token) throws Exception {
            return send("POST", SESSION, token, "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8));
        }

        private HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, null);
        }

        private HttpResponse<String> send(String method, String path, Token token, String data) throws Exception {
            return send(method, path, token, data, "application/x-www-form-urlencoded");
        }

        private HttpResponse<String> json(String path, Token token, String data) throws Exception {
            return send("POST", path, token, data, "application/json");
        }

        private HttpResponse<String> send(String method, String path, Token token, String data, String mediaType)
                throws Exception {
            var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5));
            if (data != null) {
                request.header("Content-Type", mediaType);
            }
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            if (token != null) {
                request.header(token.header(), token.value());
            }
            var body = data == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(data);
            var response = client.send(request.method(method, body).build(), HttpResponse.BodyHandlers.ofString());
            for (String setCookie : response.headers().allValues("Set-Cookie")) {
                var parsed = HttpCookie.parse(setCookie).getFirst();
                cookie = parsed.getMaxAge() == 0 ? null : setCookie.split(";", 2)[0];
            }
            return response;
        }
    }

    @RestController
    static class HttpFixture {
        private final AtomicInteger writes = new AtomicInteger();

        @GetMapping(FIXTURE)
        String read(HttpSession session) {
            return writes.get() + ":" + session.getMaxInactiveInterval();
        }

        @PostMapping(FIXTURE)
        int write() {
            return writes.incrementAndGet();
        }

        @PostMapping(FIXTURE + "/expirar")
        void expiresSoon(HttpSession session) {
            session.setMaxInactiveInterval(1);
        }

        @GetMapping("/api/public/control-prueba")
        String publicAccess() {
            return "publico";
        }

        @PostMapping("/api/public/control-prueba")
        String publicWrite() {
            return "publico";
        }

        @GetMapping("/api/gestion/control-prueba")
        String managementFamily(HttpServletRequest request) {
            return request.getUserPrincipal() == null ? "gestion" : "identidad interna inesperada";
        }
    }
}
