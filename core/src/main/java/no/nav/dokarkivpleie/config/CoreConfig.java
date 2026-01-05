package no.nav.dokarkivpleie.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableConfigurationProperties(value = {
		DokarkivpleieProperties.class,
		NaisProperties.class
})
@EnableResilientMethods
public class CoreConfig {
}