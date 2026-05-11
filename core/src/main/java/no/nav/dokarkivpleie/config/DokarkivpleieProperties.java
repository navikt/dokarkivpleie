package no.nav.dokarkivpleie.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("dokarkivpleie")
public class DokarkivpleieProperties {

	@Valid
	private final Skass001 skass001 = new Skass001();

	public static class Skass001 {
		@Getter
		@Setter
		@NotEmpty
		private String cronschedule;
	}

	@Valid
	private final Endpoints endpoints = new Endpoints();

	@Getter
	@Setter
	public static class Endpoints {
		@Valid
		@NotNull
		private AzureEndpoint pdl;

		@Valid
		@NotNull
		private Endpoint datavarehus;
	}

	@Getter
	@Setter
	public static class AzureEndpoint {
		@NotEmpty
		private String url;

		@NotEmpty
		private String scope;
	}

	@Getter
	@Setter
	public static class Endpoint {
		@NotEmpty
		private String url;
	}
}