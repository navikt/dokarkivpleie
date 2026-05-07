package no.nav.dokarkivpleie;

import jakarta.persistence.EntityManager;
import no.nav.dokarkivpleie.config.CoreConfig;
import no.nav.dokarkivpleie.config.RepositoryConfig;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusConsumer;
import no.nav.dokarkivpleie.domain.Avleveringsstatus;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.domain.Sak;
import no.nav.dokarkivpleie.domain.Saksstatus;
import no.nav.dokarkivpleie.domain.Slettebestilling;
import no.nav.dokarkivpleie.repository.FagomraadeRepository;
import no.nav.dokarkivpleie.repository.SakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.wiremock.spring.EnableWireMock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokarkivpleie.MerkSakerBevaringstidPassertService.DOKARKIVPLEIE;
import static no.nav.dokarkivpleie.MerkSakerBevaringstidPassertService.MERK_SAKER_BEVARINGSTID_PASSERT;
import static no.nav.dokarkivpleie.consumers.dvh.DatavarehusConsumer.DVH_QUERY;
import static no.nav.dokarkivpleie.consumers.dvh.DatavarehusConsumer.MAX_ANTALL_ENHETER_SOM_SKAL_HENTES;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.BEVARINGSTID_PASSERT_DOK_KASSASJON_BESTILT;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.KLAR_FOR_KASSASJON;
import static no.nav.dokarkivpleie.domain.Saksstatus.AAPEN;
import static no.nav.dokarkivpleie.domain.Saksstatus.AVBRUTT;
import static no.nav.dokarkivpleie.domain.Saksstatus.AVSLUTTET;
import static no.nav.dokarkivpleie.domain.SlettebestillingArsak.BEVARINGSTID;
import static no.nav.dokarkivpleie.domain.SlettebestillingHjemmel.ARK;
import static no.nav.dokarkivpleie.domain.SlettebestillingStatus.OPPRETTET;
import static no.nav.dokarkivpleie.domain.SlettebestillingType.DOKUMENTER_PA_SAK;
import static org.assertj.core.api.Assertions.assertThat;
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
	private static final String FAGOMRAADE_BIL = "BIL";
	private static final String FAGOMRAADE_UFM = "UFM";
	private static final String FAGOMRAADE_UFO = "UFO";
	private static final String FAGOMRAADE_GEN = "GEN";
	private static final LocalDate DOEDSDATO_MER_ENN_10_AAR_SIDEN = LocalDate.now().minusMonths(121);

	@Autowired
	private FagomraadeRepository fagomraadeRepository;

	@Autowired
	private SakRepository sakRepository;

	@Autowired
	private MerkSakerBevaringstidPassertScheduler merkSakerBevaringstidPassertScheduler;

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
		entityManager
				.createQuery("delete from Slettebestilling")
				.executeUpdate();
	}

	@Test
	void skalHoppeOverArkivsakMedBaadeAapneOgLukkedeSaker() {
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> sakerSomSkalLagres = List.of(
				Sak.builder().sakId(123L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").fagsakNr("123").saksstatus(AAPEN).brukerId("07417813777").brukerIdType("FNR").doedsdato(DOEDSDATO_MER_ENN_10_AAR_SIDEN).build(),
				Sak.builder().sakId(124L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").fagsakNr("123").saksstatus(AVSLUTTET).brukerId("07417813777").brukerIdType("FNR").doedsdato(DOEDSDATO_MER_ENN_10_AAR_SIDEN).build(),
				Sak.builder().sakId(125L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").fagsakNr("321").saksstatus(AAPEN).brukerId("07417813777").brukerIdType("FNR").doedsdato(DOEDSDATO_MER_ENN_10_AAR_SIDEN).build()
		);
		sakRepository.persistAll(sakerSomSkalLagres);
		reinitTransaction();

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		List<Sak> saker = finnAlleSaker();
		assertThat(saker)
				.extracting(Sak::getSakId, Sak::getSaksstatus, Sak::getKassasjonsstatus, Sak::getEndretAv, Sak::getEndretKildeNavn)
				.containsExactlyInAnyOrder(
						tuple(123L, AAPEN, null, null, null),
						tuple(124L, AVSLUTTET, null, null, null),
						tuple(125L, AVBRUTT, KLAR_FOR_KASSASJON, MERK_SAKER_BEVARINGSTID_PASSERT, DOKARKIVPLEIE)
				);

		assertThat(antallSlettebestillinger()).isEqualTo(0);
	}

	// SakId 345 har en journalpost i status M (ligger i schema.sql)
	@Test
	void skalAvslutteBehandlingAvArkivsakMedJournalposterIMidlertidigeStatuser() {
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(345L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).brukerId("07417813777").brukerIdType("FNR").doedsdato(DOEDSDATO_MER_ENN_10_AAR_SIDEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		Sak sak = sakRepository.findById(345L).get();

		assertThat(sak.getSaksstatus()).isEqualTo(AAPEN);
		assertThat(sak.getKassasjonsstatus()).isNull();
		assertThat(sak.getEndretAv()).isNull();
		assertThat(sak.getDatoEndret()).isNull();
	}

	// SakId 346 har en journalpost i status U
	// Lag ein journalpost med status U, A eller UB slik at ein unngår harJournalposterIMidlertidigeStatuser-sjekken
	@Test
	void skalAvbryteUtenAaKassereForTomArkivsak() {
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(346L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).brukerId("07417813777").brukerIdType("FNR").doedsdato(DOEDSDATO_MER_ENN_10_AAR_SIDEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		Sak sak = sakRepository.findById(346L).get();
		assertThat(sak.getSaksstatus()).isEqualTo(AVBRUTT);
		assertThat(sak.getAvleveringsstatus()).isEqualTo(Avleveringsstatus.AVBRUTT);
		assertThat(sak.getKassasjonsstatus()).isEqualTo(KLAR_FOR_KASSASJON);
		assertThat(sak.getEndretAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretKildeNavn()).isEqualTo(DOKARKIVPLEIE);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));

		assertThat(hentSlettebestillinger()).hasSize(0);
	}

	@Test
	public void skalAvbryteBehandlingAvArkivsakUtenAdministrativEnhet() {
		stubDvh("response.json");
		lagFagomraader();

		sakRepository.persist(Sak.builder().sakId(9999L).tema(FAGOMRAADE_AAP).aktoerId("2556016505784").applikasjon("FS22").opprettetTidspunkt(LocalDateTime.now().minusYears(50)).saksstatus(AAPEN).build());
		reinitTransaction();

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		Sak sak = sakRepository.findById(9999L).get();

		assertThat(sak.getSaksstatus()).isEqualTo(AAPEN);
		assertThat(sak.getKassasjonsstatus()).isNull();
		assertThat(sak.getEndretAv()).isNull();
		assertThat(sak.getDatoEndret()).isNull();
	}

	@Test
	void skalAvslutteOgKassereSakMedKunFerdigstilteJournalposterDerAvleverMedDokErFalse() {
		stubDvh("response.json");
		lagFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(123L).tema(FAGOMRAADE_ENF).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).brukerId("07417813777").brukerIdType("FNR").doedsdato(DOEDSDATO_MER_ENN_10_AAR_SIDEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		Sak sak = sakRepository.findById(123L).get();
		assertThat(sak.getSaksstatus()).isEqualTo(Saksstatus.AVSLUTTET);
		assertThat(sak.getDatoAvsluttet()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));
		assertThat(sak.getAvsluttetAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getAvsluttetKildeNavn()).isEqualTo(DOKARKIVPLEIE);
		assertThat(sak.getKassasjonsstatus()).isEqualTo(BEVARINGSTID_PASSERT_DOK_KASSASJON_BESTILT);
		assertThat(sak.getEndretAv()).isEqualTo(MERK_SAKER_BEVARINGSTID_PASSERT);
		assertThat(sak.getEndretKildeNavn()).isEqualTo(DOKARKIVPLEIE);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));

		assertThat(hentSlettebestillinger()).hasSize(1)
				.extracting("sakId", "slettebestillingStatus", "slettebestillingType", "slettebestillingHjemmel", "slettebestillingArsak")
				.containsExactly(tuple(123L, OPPRETTET, DOKUMENTER_PA_SAK, ARK, BEVARINGSTID));
	}

	@Test
	void skalBrukeLimitMotDvh() {
		stubDvh("response.json");

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		verify(1, getRequestedFor(urlPathEqualTo("/dvh"))
				.withQueryParam("q", equalTo(DVH_QUERY))
				.withQueryParam("limit", equalTo(String.valueOf(MAX_ANTALL_ENHETER_SOM_SKAL_HENTES)))
		);
	}

	@Test
	void skalIkkeGiRetryVedFunksjonellFeilMotDvh() {
		stubDvh(BAD_REQUEST);

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		verify(1, getRequestedFor(urlPathEqualTo("/dvh")));
	}

	@Test
	void skalAvslutteKjoeringenEtterRetriesVedTekniskFeilMotDvh() {
		stubDvh(INTERNAL_SERVER_ERROR);

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		verify(4, getRequestedFor(urlPathEqualTo("/dvh")));
	}

	@Test
	void skalIkkeOppdatereSakerHvisFagomraadetErUgyldig() {
		stubDvh("response.json");
		lagUgyldigeFagomraader();

		List<Sak> saker = List.of(
				Sak.builder().sakId(123L).tema(FAGOMRAADE_BIL).aktoerId("2425192326667").applikasjon("FS22").saksstatus(AAPEN).build(),
				Sak.builder().sakId(124L).tema(FAGOMRAADE_UFM).aktoerId("2556016505784").applikasjon("FS22").saksstatus(AAPEN).build(),
				Sak.builder().sakId(125L).tema(FAGOMRAADE_UFO).aktoerId("2425192326667").applikasjon("FS22").saksstatus(AAPEN).build(),
				Sak.builder().sakId(126L).tema(FAGOMRAADE_GEN).aktoerId("2376241635675").applikasjon("FS22").saksstatus(AAPEN).build()
		);
		sakRepository.persistAll(saker);
		reinitTransaction();

		merkSakerBevaringstidPassertScheduler.kjoerPeriodiskJobb();

		List<Sak> sakerSomIkkeSkalVaereOppdaterte = finnAlleSaker();
		assertThat(sakerSomIkkeSkalVaereOppdaterte)
				.extracting(Sak::getDatoEndret, Sak::getEndretAv)
				.containsOnly(tuple(null, null));
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

	void lagUgyldigeFagomraader() {
		//fagomraade finnes ikke i db
		fagomraadeRepository.persist(new Fagomraade(FAGOMRAADE_GEN, "10_AAR_ETTER_BRUKERS_DOED", false));
		//null er en ugyldig bevaringstid
		fagomraadeRepository.persist(new Fagomraade(FAGOMRAADE_UFO, null, false));
		//100 aar etter brukers død er ikke støtta
		fagomraadeRepository.persist(new Fagomraade(FAGOMRAADE_UFM, "100_AAR_ETTER_BRUKERS_DOED", false));
		//avleverMedDok=null er ulovlig
		fagomraadeRepository.persist(new Fagomraade(FAGOMRAADE_BIL, "10_AAR_ETTER_BRUKERS_DOED", null));

		reinitTransaction();
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

	private Long antallSlettebestillinger() {
		return (Long) entityManager
				.createQuery("select count(s) from Slettebestilling s")
				.getSingleResult();
	}

	private List<Slettebestilling> hentSlettebestillinger() {
		return entityManager
				.createQuery("select s from Slettebestilling s", Slettebestilling.class)
				.getResultList();
	}

	private List<Sak> finnAlleSaker() {
		return entityManager
				.createQuery("select s from Sak s", Sak.class)
				.getResultList();
	}
}