package dev.odontologia.config;

import java.net.URI;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(URI baseUrl, ZoneId timeZone, String consultorioNombre, String odontologoNombre) {
}
