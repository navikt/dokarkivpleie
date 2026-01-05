package no.nav.dokarkivpleie.consumers.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.config.DokarkivpleieProperties;
import no.nav.dokarkivpleie.consumers.pdl.HentPersonBolkResponse.HentPersonBolk;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.dokarkivpleie.consumers.entra.NaisTexasRequestInterceptor.ENTRA_TARGET_SCOPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class PdlConsumer {

	private static final String HEADER_NAV_CALL_ID = "Nav-Call-Id";
	private static final String HEADER_BEHANDLINGSNUMMER = "behandlingsnummer";
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";

	private final String scopePdl;
	private final RestClient restClientTexas;

	public PdlConsumer(RestClient restClientTexas,
					   DokarkivpleieProperties dokarkivpleieProperties) {
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(dokarkivpleieProperties.getEndpoints().getPdl().getUrl())
				.defaultHeaders(headers -> {
					headers.setContentType(APPLICATION_JSON);
					headers.set(HEADER_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER);
					headers.set(HEADER_NAV_CALL_ID, UUID.randomUUID().toString());
				})
				.build();
		this.scopePdl = dokarkivpleieProperties.getEndpoints().getPdl().getScope();
	}

	@Retryable(value = PdlTechnicalException.class)
	public List<HentPersonBolk> hentNyesteFnrOgDoedsdato(List<String> aktoerIder) {
		HentPersonBolkResponse pdlResponse = restClientTexas.post()
				.attribute(ENTRA_TARGET_SCOPE, scopePdl)
				.body(mapHentPersonBolk(aktoerIder))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (_, res) -> {
					if (res.getStatusCode().is4xxClientError()) {
						throw new PdlFunctionalException(format("Klarte ikke hente aktoerIder fra PDL med statuskode=%s og feilmelding=%s", res.getStatusCode(), res.getStatusText()));
					}
					throw new PdlTechnicalException(format("Klarte ikke hente aktoerIder fra PDL med statuskode=%s og feilmelding=%s", res.getStatusCode(), res.getStatusText()));
				})
				.body(HentPersonBolkResponse.class);

		if (pdlResponse.errors() == null || pdlResponse.errors().isEmpty()) {
			log.info("hentNyesteFnrOgDoedsdato har hentet svar fra PDL");
			return pdlResponse.data().hentPersonBolk();
		} else {
			throw new PdlFunctionalException("Kunne ikke hente aktørider for folkeregisterident i pdl. " + pdlResponse.errors());
		}
	}

	private PdlRequest mapHentPersonBolk(List<String> aktoerIder) {
		HashMap<String, Object> variables = new HashMap<>();
		variables.put("identer", aktoerIder);

		return PdlRequest.builder()
				.query("""
						query hentPersonBolk($identer: [ID!]!) {
						  hentPersonBolk(identer: $identer) {
						    ident
						    person {
						      doedsfall {
						        doedsdato
						      }
						      folkeregisteridentifikator {
						        identifikasjonsnummer
						        status
						        type
						      }
						    }
						    code
						  }
						}
						""")
				.variables(variables)
				.build();
	}

}