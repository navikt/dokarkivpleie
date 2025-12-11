package no.nav.dokarkivpleie.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "no.nav.dokarkivpleie.domain")
@EnableJpaRepositories(basePackages = "no.nav.dokarkivpleie.repository")
public class RepositoryConfig {
}