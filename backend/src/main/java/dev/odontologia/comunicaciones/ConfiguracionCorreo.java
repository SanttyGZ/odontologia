package dev.odontologia.comunicaciones;

import java.time.Clock;

import dev.odontologia.config.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
@EnableScheduling
class ConfiguracionCorreo {
    @Bean
    SondeoAvisos sondeoAvisos(JdbcTemplate jdbc, AutorizacionDespacho autorizacion,
            AppProperties configuracion, Clock reloj, @Value("${spring.mail.password:}") String clave) {
        var composicion = new ComposicionAvisos(configuracion.baseUrl(), configuracion.timeZone());
        var emision = new EmisionAviso(autorizacion, composicion, new SmtpGmail(clave), reloj);
        return new SondeoAvisos(new ColaDespachos(jdbc), autorizacion, emision, reloj);
    }
}
