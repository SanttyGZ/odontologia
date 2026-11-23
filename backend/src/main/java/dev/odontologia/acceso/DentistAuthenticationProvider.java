package dev.odontologia.acceso;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.odontologia.consultorio.ConsultorioQueries;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

final class DentistAuthenticationProvider implements AuthenticationProvider {

    private final Optional<DentistCredentialFile.Credential> credential;
    private final ConsultorioQueries consultations;

    DentistAuthenticationProvider(Optional<DentistCredentialFile.Credential> credential, ConsultorioQueries consultations) {
        this.credential = credential;
        this.consultations = consultations;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        var account = credential.orElseThrow(AccessNotConfigured::new);
        if (!(authentication.getCredentials() instanceof String password)
                || !account.matches(authentication.getName(), password)) {
            throw new BadCredentialsException("Credencial inválida.");
        }

        UUID dentistId;
        try {
            dentistId = consultations.findConfiguration()
                    .orElseThrow(() -> new AuthenticationServiceException("Odontólogo no disponible."))
                    .odontologo().id();
        } catch (DataAccessException failure) {
            throw new AuthenticationServiceException("Odontólogo no disponible.");
        }
        var principal = new DentistPrincipal(dentistId, authentication.getName());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO")));
    }

    @Override
    public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }

    static final class AccessNotConfigured extends AuthenticationServiceException {
        AccessNotConfigured() {
            super("Acceso interno sin configurar.");
        }
    }

    private record DentistPrincipal(UUID id, String username) implements Principal {
        @Override
        public String getName() {
            return username;
        }
    }
}
