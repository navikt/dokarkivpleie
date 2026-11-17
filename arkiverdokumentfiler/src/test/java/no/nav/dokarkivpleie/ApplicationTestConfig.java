package no.nav.dokarkivpleie;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("itest")
@EnableAutoConfiguration
@ComponentScan(basePackages = "no.nav.dokarkivpleie")
public class ApplicationTestConfig {
}
