package no.nav.dokarkivpleie.consumers.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.config.DokarkivpleieProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DatavarehusConsumer {

	private final RestClient restClient;
	private static final String QUERY = """
			{"mapping_node_type":{"$or":[{"$eq":"ARENAENHET"},{"$eq":"INFOENHET"},{"$eq":"NORGENHET"}]}}
			""";

	public DatavarehusConsumer(DokarkivpleieProperties dokarkivpleieProperties,
							   RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder
				.baseUrl(dokarkivpleieProperties.getEndpoints().getDatavarehus().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(value = {DatavarehusTechnicalException.class, RestClientException.class}, delayString = "${dvh.retry.delay:1000}")
	public DatavarehusResponse hentAlleAdministrativeEnheter() {
		return restClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromUri(uriBuilder.build())
						.queryParam("q", QUERY)
						.build()
						.toUri())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (_, response) -> {
					if (response.getStatusCode().is4xxClientError()) {
						throw new DatavarehusFunctionalException(format("Funksjonell feil ved henting av henting av navn for administrativ enhet fra DVH. Feilmelding=%s", response.getStatusText()));
					}
					throw new DatavarehusTechnicalException(format("Teknisk feil ved henting av henting av navn for administrativ enhet fra DVH. Feilmelding=%s", response.getStatusText()));
				})
				.body(DatavarehusResponse.class);
	}
}
