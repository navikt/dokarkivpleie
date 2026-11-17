package no.nav.dokarkivpleie.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
	private final Endpoints endpoints = new Endpoints();
	@Valid
	private final DokarkivpleieProperties.Arkiverdokumentfiler arkiverDokumentfiler = new Arkiverdokumentfiler();

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

	@Getter
	@Setter
	public static class Arkiverdokumentfiler {
		@NotBlank
		String cronschedule;
		@NotBlank
		String bucket;
		@Min(1)
		int batchStorrelse = 50;
		@Min(1)
		int maksForsok = 5;
		@Min(1)
		int maksSamtidige = 5;
		@Min(1)
		int transaksjonTimeout = 300;
	}
}