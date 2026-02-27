package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.domain.Sak;
import no.nav.dokarkivpleie.domain.Saksstatus;
import no.nav.dokarkivpleie.repository.SakRepository;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokarkivpleie.domain.Saksstatus.AAPEN;
import static no.nav.person.pdl.leesah.Endringstype.ANNULLERT;
import static no.nav.person.pdl.leesah.Endringstype.KORRIGERT;
import static no.nav.person.pdl.leesah.Endringstype.OPPRETTET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class Karp001ITest extends AbstractKafkaBrokerTest {

	private static final String LEESAH_KAFKA_TOPIC = "pdl.leesah-v1";
	private static final String HENDELSE_DOEDSFALL = "DOEDSFALL_V1";
	private static final String HENDELSE_FOEDSEL = "FOEDSEL_V1";
	private static final String AKTOERID_1 = "2556016505784";
	private static final String AKTOERID_2 = "1556016505799";
	private static final String AKTOERID_3 = "0556016505799";
	private static final String NPID = "17629124853";
	private static final String ENDRET_AV = "OppdaterSakerMedDoedsdatoOgFnr";
	private static final String FNR = "FNR";

	private static final String FNR_FOR_AKTOERID_1_OG_2 = "07417813777";
	private static final LocalDate DOEDSDATO = LocalDate.of(2024, 1, 15);

	@Autowired
	private KafkaTemplate<String, Personhendelse> kafkaTemplate;

	@Autowired
	private SakRepository sakRepository;

	@BeforeEach
	void setUp() {
		sakRepository.deleteAll();
	}

	@Test
	void skalOppdatereFoedselsnummerOgDoedsdatoForEndringstypeOpprett() {
		stubNaisTexasToken();
		stubPdl("hentidenter.json");
		lagSaker();

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(AKTOERID_2), DOEDSDATO);
		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-123", personhendelse);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			List<Sak> oppdaterteSaker = sakRepository.findAllById(List.of(123L, 124L));
			assertThat(oppdaterteSaker)
					.hasSize(2)
					.extracting(Sak::getBrukerId, Sak::getBrukerIdType, Sak::getDoedsdato, Sak::getEndretAv)
					.containsOnly(tuple(FNR_FOR_AKTOERID_1_OG_2, FNR, DOEDSDATO, ENDRET_AV));

			Sak sakSomIkkeErOppdatert = sakRepository.findById(125L).get();
			assertThat(sakSomIkkeErOppdatert)
					.extracting(Sak::getDoedsdato, Sak::getBrukerId, Sak::getBrukerIdType, Sak::getEndretAv, Sak::getDatoEndret)
					.containsOnlyNulls();
		});
	}

	@Test
	void skalOppdatereFoedselsnummerOgDoedsdatoForEndringstypeOpprettForNPID() {
		lagSaker();
		stubNaisTexasToken();
		stubPdl("npid_uten_nyeste_fnr.json");

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(AKTOERID_1), DOEDSDATO);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			Sak oppdatertSak = sakRepository.findById(123L).orElseThrow();
			assertThat(oppdatertSak)
					.extracting(Sak::getBrukerId, Sak::getBrukerIdType, Sak::getDoedsdato, Sak::getEndretAv)
					.containsExactly(NPID, FNR, DOEDSDATO, ENDRET_AV);
			verify(1, postRequestedFor(urlEqualTo("/pdl")));
		});
	}

	@Test
	void skalOppdatereFoedselsnummerOgDoedsdatoForEndringstypeKorriger() {
		stubNaisTexasToken();
		stubPdl("hentidenter.json");
		var doedsdatoFoerKorrigering = LocalDate.now();
		sakRepository.saveAll(List.of(
				lagSak(123L, AKTOERID_1, AAPEN).brukerIdType(FNR).brukerId(FNR_FOR_AKTOERID_1_OG_2).doedsdato(doedsdatoFoerKorrigering).build(),
				lagSak(124L, AKTOERID_1, AAPEN).build()));
		reinitTransaction();

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(FNR_FOR_AKTOERID_1_OG_2, AKTOERID_1), DOEDSDATO, KORRIGERT);
		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-123", personhendelse);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			List<Sak> oppdaterteSaker = sakRepository.findAllById(List.of(123L, 124L));
			assertThat(oppdaterteSaker)
					.hasSize(2)
					.extracting(Sak::getBrukerId, Sak::getBrukerIdType, Sak::getDoedsdato, Sak::getEndretAv)
					.containsOnly(tuple(FNR_FOR_AKTOERID_1_OG_2, FNR, DOEDSDATO, ENDRET_AV));
		});
	}

	@Test
	void skalAnnullereDoedsdatoForEndringstypeAnnullert() {
		stubNaisTexasToken();
		stubPdl("hentidenter.json");
		sakRepository.save(lagSak(123L, AKTOERID_1, AAPEN).doedsdato(LocalDate.now()).build());
		reinitTransaction();

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(FNR_FOR_AKTOERID_1_OG_2, AKTOERID_1), DOEDSDATO, ANNULLERT);
		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-123", personhendelse);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			List<Sak> oppdaterteSaker = sakRepository.findAllById(List.of(123L));
			assertThat(oppdaterteSaker)
					.hasSize(1)
					.extracting(Sak::getBrukerId, Sak::getDoedsdato, Sak::getEndretAv)
					.containsOnly(tuple(null, null, ENDRET_AV));
		});
	}

	@Test
	void skalIgnorereHendelserSomIkkeErDoedsfall() {
		lagSaker();

		Personhendelse personhendelse = lagLeesahHendelse(List.of(AKTOERID_2), HENDELSE_FOEDSEL);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().during(2, SECONDS).atMost(5, SECONDS).untilAsserted(() -> {
			Sak uendretSak = sakRepository.findById(123L).orElseThrow();
			assertThat(uendretSak)
					.extracting(Sak::getDoedsdato, Sak::getBrukerId, Sak::getBrukerIdType, Sak::getEndretAv, Sak::getDatoEndret)
					.containsOnlyNulls();
		});
	}

	@Test
	void skalIgnorereDoedsfallUtenPersonidenter() {
		stubNaisTexasToken();
		stubPdl("hentidenter.json");

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(), DOEDSDATO);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().during(2, SECONDS).atMost(5, SECONDS).untilAsserted(() -> {
			verify(0, postRequestedFor(urlEqualTo("/pdl")));
		});
	}

	@Test
	void skalIgnorereDoedsfallUtenDoedsdato() {
		stubNaisTexasToken();
		stubPdl("hentidenter.json");

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(AKTOERID_2), null);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().during(2, SECONDS).atMost(5, SECONDS).untilAsserted(() -> {
			verify(0, postRequestedFor(urlEqualTo("/pdl")));
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"server_error.json", "unauthorized.json", "unauthenticated.json"})
	@DirtiesContext
	void skalLeseInnKafkameldingPaaNyttVedTekniskFeilMotPdl(String pdlResponse) {
		stubNaisTexasToken();
		stubPdl(pdlResponse);

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(AKTOERID_2), DOEDSDATO);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			verify(moreThanOrExactly(3), postRequestedFor(urlEqualTo("/pdl")));
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"bad_request.json", "not_found.json"})
	void skalAvslutteProsesseringVedFunksjonellFeilMotPdl(String pdlResponse) {
		lagSaker();
		stubNaisTexasToken();
		stubPdl(pdlResponse);

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(AKTOERID_1), DOEDSDATO);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().during(2, SECONDS).atMost(5, SECONDS).untilAsserted(() -> {
			Sak uendretSak = sakRepository.findById(123L).orElseThrow();
			assertThat(uendretSak)
					.extracting(Sak::getDoedsdato, Sak::getBrukerId, Sak::getBrukerIdType, Sak::getEndretAv, Sak::getDatoEndret)
					.containsOnlyNulls();
			verify(1, postRequestedFor(urlEqualTo("/pdl")));
		});
	}

	@Test
	void skalAvslutteHvisIngenFnrEllerNpidFraPdl() {
		lagSaker();
		stubNaisTexasToken();
		stubPdl("kun_aktoerid.json");

		Personhendelse personhendelse = lagLeesahDoedsfallHendelse(List.of(AKTOERID_1), DOEDSDATO);

		kafkaTemplate.send(LEESAH_KAFKA_TOPIC, "key-456", personhendelse);

		await().during(2, SECONDS).atMost(5, SECONDS).untilAsserted(() -> {
			Sak uendretSak = sakRepository.findById(123L).orElseThrow();
			assertThat(uendretSak)
					.extracting(Sak::getDoedsdato, Sak::getBrukerId, Sak::getBrukerIdType, Sak::getEndretAv, Sak::getDatoEndret)
					.containsOnlyNulls();
			verify(1, postRequestedFor(urlEqualTo("/pdl")));
		});
	}

	private Personhendelse lagLeesahDoedsfallHendelse(List<CharSequence> personidenter, LocalDate doedsdato, Endringstype endringstype) {
		Personhendelse leesahHendelse = lagLeesahHendelse(personidenter, HENDELSE_DOEDSFALL, endringstype);
		leesahHendelse.setDoedsfall(new Doedsfall(doedsdato));

		return leesahHendelse;
	}

	private Personhendelse lagLeesahDoedsfallHendelse(List<CharSequence> personidenter, LocalDate doedsdato) {
		return lagLeesahDoedsfallHendelse(personidenter, doedsdato, OPPRETTET);
	}

	private Personhendelse lagLeesahHendelse(List<CharSequence> personidenter, String opplysningstype) {
		return lagLeesahHendelse(personidenter, opplysningstype, OPPRETTET);
	}

	private Personhendelse lagLeesahHendelse(List<CharSequence> personidenter, String opplysningstype, Endringstype endringstype) {
		Personhendelse personhendelse = new Personhendelse();
		personhendelse.setHendelseId("hendelse-" + System.currentTimeMillis());
		personhendelse.setOpplysningstype(opplysningstype);
		personhendelse.setPersonidenter(personidenter);
		personhendelse.setMaster("PDL");
		personhendelse.setOpprettet(Instant.now());
		personhendelse.setEndringstype(endringstype);

		return personhendelse;
	}

	private void lagSaker() {
		sakRepository.saveAll(List.of(
				lagSak(123L, AKTOERID_1, AAPEN).build(),
				lagSak(124L, AKTOERID_2, AAPEN).build(),
				lagSak(125L, AKTOERID_3, AAPEN).build()
		));
		reinitTransaction();
	}

	private Sak.SakBuilder lagSak(Long sakId, String aktoerId, Saksstatus saksstatus) {
		return Sak.builder().sakId(sakId).aktoerId(aktoerId).applikasjon("FS22").saksstatus(saksstatus);
	}

	void stubPdl(String filename) {
		stubFor(post(urlEqualTo("/pdl"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/" + filename)));
	}

	void stubNaisTexasToken() {
		stubFor(post("/texas-token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("nais-texas/texas_response.json")));
	}

}