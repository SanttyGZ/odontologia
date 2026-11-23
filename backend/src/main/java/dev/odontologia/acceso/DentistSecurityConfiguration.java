package dev.odontologia.acceso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import dev.odontologia.consultorio.ConsultorioQueries;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class DentistSecurityConfiguration {

    static final String API = "/api/odontologo";
    static final String SESSION = API + "/sesion";
    static final String CSRF = API + "/csrf";

    @Bean
    DentistAuthenticationProvider dentistAuthenticationProvider(
            @Value("${app.dentist-credential-file:}") String path, ConsultorioQueries consultations) {
        Optional<DentistCredentialFile.Credential> credential = Optional.empty();
        if (!path.isBlank()) {
            try {
                credential = DentistCredentialFile.read(Path.of(path));
            } catch (InvalidPathException ignored) {
                // Una ruta inválida deja deshabilitado el ingreso.
            }
        }
        return new DentistAuthenticationProvider(credential, consultations);
    }

    @Bean
    SecurityFilterChain dentistSecurityFilterChain(HttpSecurity http, DentistAuthenticationProvider provider) {
        return http.securityMatcher(API + "/**")
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(HttpMethod.GET, CSRF).permitAll()
                        .requestMatchers(HttpMethod.POST, SESSION).permitAll()
                        .anyRequest().hasRole("ODONTOLOGO"))
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sessions -> sessions.sessionFixation(fixation -> fixation.changeSessionId()))
                .formLogin(login -> login.loginPage(SESSION).loginProcessingUrl(SESSION)
                        .successHandler((request, response, authentication) -> noContent(response))
                        .failureHandler((request, response, failure) -> {
                            if (failure instanceof DentistAuthenticationProvider.AccessNotConfigured) {
                                error(response, 503, "ACCESS_NOT_CONFIGURED");
                            } else if (failure instanceof AuthenticationServiceException) {
                                error(response, 503, "ACCESS_UNAVAILABLE");
                            } else {
                                error(response, 401, "INVALID_CREDENTIALS");
                            }
                        }))
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, failure) -> error(response, 401, "SESSION_REQUIRED"))
                        .accessDeniedHandler((request, response, failure) -> error(response, 403,
                                failure instanceof CsrfException ? "CSRF_INVALID" : "ACCESS_DENIED")))
                .addFilterBefore(new LoginFormFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static void noContent(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static void error(HttpServletResponse response, int status, String code) throws IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(status);
        response.getWriter().write("{\"codigo\":\"" + code + "\"}");
    }

    private static final class LoginFormFilter extends OncePerRequestFilter {
        private final PathPatternRequestMatcher login = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, SESSION);

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !login.matches(request);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (request.getQueryString() != null || !isForm(request.getContentType())) {
                error(response, 400, "INVALID_LOGIN_REQUEST");
                return;
            }
            chain.doFilter(request, response);
        }

        private static boolean isForm(String contentType) {
            try {
                return contentType != null && MediaType.APPLICATION_FORM_URLENCODED.includes(
                        MediaType.parseMediaType(contentType));
            } catch (InvalidMediaTypeException invalid) {
                return false;
            }
        }
    }
}
