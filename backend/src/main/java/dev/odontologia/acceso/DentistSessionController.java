package dev.odontologia.acceso;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static dev.odontologia.acceso.DentistSecurityConfiguration.CSRF;
import static dev.odontologia.acceso.DentistSecurityConfiguration.SESSION;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class DentistSessionController {

    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    @GetMapping(CSRF)
    ResponseEntity<CsrfResponse> csrf(CsrfToken csrf) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(csrf.getToken(), csrf.getHeaderName()));
    }

    @GetMapping(SESSION)
    ResponseEntity<SessionResponse> session() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SessionResponse(true));
    }

    @PostMapping(SESSION + "/cierre")
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        logout.logout(request, response, authentication);

        var configuration = request.getServletContext().getSessionCookieConfig();
        Cookie expired = new Cookie(configuration.getName(), "");
        expired.setPath(configuration.getPath());
        expired.setHttpOnly(true);
        expired.setSecure(configuration.isSecure() || request.isSecure());
        expired.setMaxAge(0);
        expired.setAttribute("SameSite", "Strict");
        response.addCookie(expired);
        DentistSecurityConfiguration.noContent(response);
    }

    record CsrfResponse(String token, String headerName) {
    }

    record SessionResponse(boolean autenticado) {
    }
}
