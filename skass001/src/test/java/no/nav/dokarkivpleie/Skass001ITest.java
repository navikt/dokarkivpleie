package no.nav.dokarkivpleie;

import jakarta.persistence.EntityManager;
import lombok.Builder;
import no.nav.dokarkivpleie.config.CoreConfig;
import no.nav.dokarkivpleie.config.RepositoryConfig;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusFunctionalException;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusTechnicalException;
import no.nav.dokarkivpleie.consumers.pdl.PdlFunctionalException;
import no.nav.dokarkivpleie.domain.Avleveringsstatus;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.domain.Sak;
import no.nav.dokarkivpleie.domain.Saksstatus;
import no.nav.dokarkivpleie.repository.AdministrativEnhetJdbcRepository;
import no.nav.dokarkivpleie.repository.FagomraadeRepository;
import no.nav.dokarkivpleie.repository.JournalpostJdbcRepository;
import no.nav.dokarkivpleie.repository.SakRepository;
import no.nav.dokarkivpleie.repository.SlettebestillingJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.wiremock.spring.EnableWireMock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokarkivpleie.MerkSakerBevaringstidPassertService.DOKARKIVPLEIE;
import static no.nav.dokarkivpleie.MerkSakerBevaringstidPassertService.MERK_SAKER_BEVARINGSTID_PASSERT;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.BEVARINGSTID_PASSERT;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.BEVARINGSTID_PASSERT_DOK_KASSASJON_BESTILT;
import static no.nav.dokarkivpleie.domain.Saksstatus.AAPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {
		ApplicationTestConfig.class,
		RepositoryConfig.class,
		CoreConfig.class
})
@Transactional
@EnableWireMock
@ActiveProfiles("itest")
public class Skass001ITest {

	private static final String FAGOMRAADE_AAP = "AAP";
	private static final String FAGOMRAADE_ENF = "ENF";

	@Autowired
	private FagomraadeRepository fagomraadeRepository;

	@Autowired
	private SakRepository sakRepository;


	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private JournalpostJdbcRepository journalpostJdbcRepository;

	@Autowired
	private MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService;

	@Autowired
	private AdministrativEnhetJdbcRepository administrativEnhetJdbcRepository;

	@Autowired
	private SlettebestillingJdbcRepository slettebestillingJdbcRepository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		entityManager
				.createQuery("delete from Sak")
				.executeUpdate();
		entityManager
				.createQuery("delete from Fagomraade ")
				.executeUpdate();

		namedParameterJdbcTemplate.update("delete from joark.t_slettebestilling", Collections.emptyMap());
	}

	@Test
	void skalKassereSakDerAvleverMedDokErTrue() {
		stubPdl("hentpersonbolk.json");
		stubNaisTexasToken();
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(123L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP);

		Sak sak = sakRepository.findById(123L).get();
		assertThat(sak.getBrukerId()).isEqualTo("07417813777");
		assertThat(sak.getDoedsdato()).isEqualTo(LocalDate.of(1900, 11, 3));

		assertThat(sak.getSaksstatus()).isEqualTo(Saksstatus.AVSLUTTET);
		assertThat(sak.getKassasjonsstatus()).isEqualTo(BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretKildeNavn()).isEqualTo(DOKARKIVPLEIE);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));

		assertThat(getSlettebestillinger()).hasSize(0);
	}

	@Test
	void skalKassereSakDerAvleverMedDokErFalse() {
		stubPdl("hentpersonbolk.json");
		stubNaisTexasToken();
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(123L).tema(FAGOMRAADE_ENF).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_ENF);

		Sak sak = sakRepository.findById(123L).get();
		assertThat(sak.getBrukerId()).isEqualTo("07417813777");
		assertThat(sak.getDoedsdato()).isEqualTo(LocalDate.of(1900, 11, 3));

		assertThat(sak.getSaksstatus()).isEqualTo(Saksstatus.AVSLUTTET);
		assertThat(sak.getKassasjonsstatus()).isEqualTo(BEVARINGSTID_PASSERT_DOK_KASSASJON_BESTILT);
		assertThat(sak.getEndretAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretKildeNavn()).isEqualTo(DOKARKIVPLEIE);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));

		assertThat(getSlettebestillinger()).hasSize(1)
				.extracting("sakId", "slettebestillingStatus", "slettebestillingType", "slettebestillingHjemmel", "slettebestillingArsak")
				.containsExactly(tuple("123", "OPPRETTET", "DOKUMENTER_PA_SAK", "ARK", "BEVARINGSTID"));
	}


//	@ParameterizedTest
//	@MethodSource
//	void skalAvslutteJobbenDersomFagomraadetErUgyldig(Fagomraade fagomraadeIDb, String fagomraadeJobbenBlirKjoertFor) {
//		stubDvh("response.json");
//		lagreSaker();
//		fagomraadeRepository.persist(fagomraadeIDb);
//
//		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(fagomraadeJobbenBlirKjoertFor);
//
//		List<Sak> saker = sakRepository.findAll();
//
//		assertThat(saker)
//				.extracting(Sak::getDatoEndret, Sak::getEndretAv)
//				.containsOnly(tuple(null, null));
//	}

	private static Stream<Arguments> skalAvslutteJobbenDersomFagomraadetErUgyldig() {
		String temaSomIkkeFinnesIDb = "BIL";
		String ugyldigBevaringstid = null;
		String ikkeStoettetBevaringstid = "100_AAR_ETTER_BRUKERS_DOED";
		Boolean avleverMedDokIkkeSatt = null;

		return Stream.of(
				Arguments.of(new Fagomraade(FAGOMRAADE_AAP, "10_AAR_ETTER_BRUKERS_DOED", true), temaSomIkkeFinnesIDb),
				Arguments.of(new Fagomraade(FAGOMRAADE_AAP, ugyldigBevaringstid, true), FAGOMRAADE_AAP),
				Arguments.of(new Fagomraade(FAGOMRAADE_AAP, ikkeStoettetBevaringstid, true), FAGOMRAADE_AAP),
				Arguments.of(new Fagomraade(FAGOMRAADE_AAP, "10_AAR_ETTER_BRUKERS_DOED", avleverMedDokIkkeSatt), FAGOMRAADE_AAP)
		);
	}

	@Test
	void skalAvslutteJobbenDersomDatavarehusKaster4xx() {
		stubDvh(BAD_REQUEST);

		assertThatExceptionOfType(DatavarehusFunctionalException.class)
				.isThrownBy(() -> merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP))
				.withMessage("Funksjonell feil ved henting av henting av navn for administrativ enhet fra DVH. Feilmelding=Bad Request");
		verify(1, getRequestedFor(urlPathEqualTo("/dvh")));
	}

	@Test
	void skalAvslutteJobbenDersomDatavarehusKaster5xx() {
		stubDvh(INTERNAL_SERVER_ERROR);

		assertThatExceptionOfType(DatavarehusTechnicalException.class)
				.isThrownBy(() -> merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP))
				.withMessage("Teknisk feil ved henting av henting av navn for administrativ enhet fra DVH. Feilmelding=Server Error");
		verify(4, getRequestedFor(urlPathEqualTo("/dvh")));
	}

	@Test
	@Disabled // Denne skal inn i kafka-modulen
	void skalAvslutteJobbenDersomPdlKaster5xx() {
		stubDvh("response.json");
		stubNaisTexasToken();
		stubPdl("unauthorized.json");
		lagFagomraader();
		lagreSaker();

		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP);

		assertThatExceptionOfType(PdlFunctionalException.class)
				.isThrownBy(() -> merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP))
				.withMessageContaining("Kunne ikke hente aktørider for folkeregisterident i pdl.");
	}

	// SakId 345 har éin journalpost i status M og ein i status FL
	// Merk at dødsdato skal bli oppdatert sjølv om saka ikkje blir avslutta
	@Test
	void skalAvslutteBehandlingAvArkivsakMedUferdigeJournalposter() {
		stubPdl("hentpersonbolk.json");
		stubNaisTexasToken();
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(345L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP);

		Sak sak = sakRepository.findById(345L).get();

		assertThat(sak.getSaksstatus()).isEqualTo(AAPEN);

		assertThat(sak.getBrukerId()).isEqualTo("07417813777");
		assertThat(sak.getDoedsdato()).isEqualTo(LocalDate.of(1900, 11, 3));
		assertThat(sak.getEndretAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));
	}

	// SakId 346 har éin journalpost i status U
	// Lag ein journalpost med status U, A eller UB slik at ein unngår harUferdigeJournalposter-sjekken
	@Test
	void skalAvbryteTomArkivsak() {
		stubPdl("hentpersonbolk.json");
		stubNaisTexasToken();
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(346L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP);

		Sak sak = sakRepository.findById(346L).get();

		assertThat(sak.getBrukerId()).isEqualTo("07417813777");
		assertThat(sak.getDoedsdato()).isEqualTo(LocalDate.of(1900, 11, 3));

		assertThat(sak.getSaksstatus()).isEqualTo(Saksstatus.AVBRUTT);
		assertThat(sak.getAvleveringsstatus()).isEqualTo(Avleveringsstatus.AVBRUTT);
		assertThat(sak.getKassasjonsstatus()).isEqualTo(BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretKildeNavn()).isEqualTo(DOKARKIVPLEIE);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));

		assertThat(getSlettebestillinger()).hasSize(0);
	}

	@Test
	public void skalAvbryteBehandlingAvArkivsakUtenAdministrativEnhet() {
		stubPdl("hentpersonbolk.json");
		stubNaisTexasToken();
		stubDvh("response.json");
		lagFagomraader();

		sakRepository.persist(Sak.builder().sakId(9999L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").opprettetTidspunkt(LocalDateTime.now().minusMonths(300)).saksstatus(AAPEN).build());
		reinitTransaction();

		merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(FAGOMRAADE_AAP);

		Sak sak = sakRepository.findById(9999L).get();

		assertThat(sak.getSaksstatus()).isEqualTo(AAPEN);
		assertThat(sak.getKassasjonsstatus()).isNull();
	}

	// Kva skal vi leggje inn i schema.sql, og kva tek vi i sakRepo?
	private void lagreSaker() {
		List<Sak> saker = List.of(
				Sak.builder().sakId(123L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).build(),
				Sak.builder().sakId(234L).tema(FAGOMRAADE_AAP).aktoerId("2376241635675").applikasjon("FS22").saksstatus(AAPEN).build(),
				Sak.builder().sakId(345L).tema(FAGOMRAADE_AAP).aktoerId("2425192326667").applikasjon("FS22").saksstatus(AAPEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();
	}

	protected void reinitTransaction() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();
	}

	void lagFagomraader() {
		fagomraadeRepository.persist(new Fagomraade(FAGOMRAADE_AAP, "10_AAR_ETTER_BRUKERS_DOED", true));
		fagomraadeRepository.persist(new Fagomraade(FAGOMRAADE_ENF, "10_AAR_ETTER_BRUKERS_DOED", false));

		reinitTransaction();
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

	void stubDvh(String filename) {
		stubFor(get(urlPathEqualTo("/dvh"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dvh/" + filename)));
	}

	void stubDvh(HttpStatus httpStatus) {
		stubFor(get(urlPathEqualTo("/dvh"))
				.willReturn(aResponse()
						.withStatus(httpStatus.value())));
	}

	private List<Slettebestilling> getSlettebestillinger() {
		return namedParameterJdbcTemplate.query(
				"select * from joark.T_SLETTEBESTILLING",
				Collections.emptyMap(),
				(rs, _) -> mapRowToSlettebestilling(rs)
		);
	}

	@Builder
	private record Slettebestilling(
			String sakId,
			String opprettetKildeNavn,
			String opprettetAvNavn,
			String opprettetAv,
			String slettebestillingStatus,
			String slettebestillingType,
			String slettebestillingHjemmel,
			String slettebestillingArsak,
			LocalDate datoUtfores,
			LocalDateTime datoOpprettet,
			String begrunnelse
	) {
	}


	private Slettebestilling mapRowToSlettebestilling(ResultSet rs) throws SQLException {
		return Slettebestilling.builder()
				.sakId(rs.getString("SAK_ID"))
				.opprettetKildeNavn(rs.getString("OPPRETTET_KILDE_NAVN"))
				.opprettetAvNavn(rs.getString("OPPRETTET_AV_NAVN"))
				.opprettetAv(rs.getString("OPPRETTET_AV"))
				.slettebestillingStatus(rs.getString("K_SLETTEBESTILLING_STATUS"))
				.slettebestillingType(rs.getString("K_SLETTEBESTILLING_TYPE"))
				.slettebestillingHjemmel(rs.getString("K_SLETTEBESTILLING_HJEMMEL"))
				.slettebestillingArsak(rs.getString("K_SLETTEBESTILLING_ARSAK"))
				.datoUtfores(rs.getDate("DATO_UTFORES") != null ? rs.getDate("DATO_UTFORES").toLocalDate() : null)
				.datoOpprettet(rs.getTimestamp("DATO_OPPRETTET") != null ? rs.getTimestamp("DATO_OPPRETTET").toLocalDateTime() : null)
				.begrunnelse(rs.getString("BEGRUNNELSE"))
				.build();
	}
}