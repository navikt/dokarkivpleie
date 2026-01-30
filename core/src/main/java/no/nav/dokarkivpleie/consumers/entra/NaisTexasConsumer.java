package no.nav.dokarkivpleie.consumers.entra;

import no.nav.dokarkivpleie.config.NaisProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Component
public class NaisTexasConsumer {

	private static final Pattern ENTRA_TARGET_PATTERN = Pattern.compile("api://[^.]+\\.[^.]+\\.[^.]+/\\.default");
	private static final String IDENTTITY_PROVIDER = "entra_id";
	private final RestClient restClient;

	public NaisTexasConsumer(RestClient.Builder restClientBuilder,
							 NaisProperties naisProperties) {
		this.restClient = restClientBuilder
				.baseUrl(naisProperties.getTokenEndpoint())
				.build();
	}

	/**
	 * Maskin-til-maskin systemtoken fra Texas
	 *
	 * @param targetScope Maskin man vil autorisere mot på format api://<cluster>.<namespace>.<other-api-app-name>/.default
	 * @return Bearer token
	 */
	public @NonNull String getSystemToken(String targetScope) {
		if (isBlank(targetScope) || !ENTRA_TARGET_PATTERN.matcher(targetScope).matches()) {
			throw new IllegalArgumentException("Ugyldig targetScope. Må være på format api://<cluster>.<namespace>.<other-api-app-name>/.default");
		}

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", IDENTTITY_PROVIDER);
		formData.add("target", targetScope);

		return requireNonNull(restClient.post()
				.contentType(APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(NaisTexasToken.class))
				.accessToken();
	}

}