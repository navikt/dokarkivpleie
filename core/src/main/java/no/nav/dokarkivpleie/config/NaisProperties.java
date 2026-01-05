package no.nav.dokarkivpleie.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("nais")
public class NaisProperties {

	// https://doc.nais.io/auth/reference/#texas
	@NotEmpty
	private String tokenEndpoint;
}
