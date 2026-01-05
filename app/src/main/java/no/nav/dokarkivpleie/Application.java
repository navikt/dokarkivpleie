package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.config.CoreConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@Import(CoreConfig.class)
public class Application {

	static void main() {
		SpringApplication.run(Application.class);
	}

}