package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.config.RepositoryConfig;
import no.nav.dokarkivpleie.domain.Sak;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.KASSERT;
import static no.nav.dokarkivpleie.domain.Saksstatus.AVSLUTTET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ContextConfiguration(classes = {RepositoryConfig.class})
class SakRepositoryTest {

	private static final String AKTOER_ID1 = "3345247044473";
	private static final String AKTOER_ID2 = "0565039150168";
	private static final String AKTOER_ID3 = "0762894254017";
	private static final String BRUKERID_FNR = "07417813777";
	private static final String BRUKERID2_FNR = "25429233464";
	private static final String BRUKERIDTYPE = "FNR";
	private static final LocalDate DOEDSDATO = LocalDate.of(2025, 12, 24);
	private static final LocalDate DOEDSDATO_OVER_10_AAR = LocalDate.now().minusMonths(121);
	private static final LocalDate DOEDSDATO_UNDER_10_AAR = LocalDate.now().minusMonths(119);
	private static final String TEMA_AAP = "AAP";
	private static final String TEMA_BIL = "BIL";
	private static final String ENDRET_AV = "MerkSakerBevaringstidPassert";

	private long sakIdCounter = 100L;

	@Autowired
	SakRepository sakRepository;

	@Test
	void skalFinneAktoeriderSomSkalFaaOppdatertFoedselsnummerOgDoedsdato() {
		var sak = createBaseSak().aktoerId(AKTOER_ID1).build();
		var sakMedSammeAktoerId = createBaseSak().aktoerId(AKTOER_ID1).build();
		var sakMedFeilTema = createBaseSak().tema(TEMA_BIL).aktoerId(AKTOER_ID3).build();
		var sakUtenAktoerId = createBaseSak().build();
		var sakMedKassasjonsstatus = createBaseSak().aktoerId(AKTOER_ID3).kassasjonsstatus(KASSERT).saksstatus(AVSLUTTET).build();
		var sakMedDoedsdato = createBaseSak().aktoerId(AKTOER_ID3).doedsdato(LocalDate.now().minusWeeks(1)).build();
		var sakMedAnnenAktoerId = createBaseSak().aktoerId(AKTOER_ID2).build();
		sakRepository.saveAll(List.of(sak, sakMedSammeAktoerId, sakMedFeilTema, sakUtenAktoerId, sakMedKassasjonsstatus, sakMedDoedsdato, sakMedAnnenAktoerId));

		Set<String> saker = sakRepository.finnAktoeriderDerFoedselsnummerOgDoedsdatoSkalBliOppdatert(TEMA_AAP);

		assertThat(saker)
				.isNotNull()
				.hasSize(2)
				.contains(AKTOER_ID1, AKTOER_ID2);
	}

	@Test
	void skalOppdatereFoedselsnummerOgDoedsdato() {
		var relevantSak = createBaseSak().aktoerId(AKTOER_ID1).build();
		var sakMedAnnetTema = createBaseSak().tema(TEMA_BIL).aktoerId(AKTOER_ID1).build();
		var sakUtenAktoerId = createBaseSak().build();
		var sakMedFeilSaksstatus = createBaseSak().aktoerId(AKTOER_ID1).saksstatus(AVSLUTTET).build();
		sakRepository.saveAll(List.of(relevantSak, sakMedAnnetTema, sakUtenAktoerId, sakMedFeilSaksstatus));

		sakRepository.oppdaterFoedselsnummerOgDoedsdato(AKTOER_ID1, BRUKERID_FNR, DOEDSDATO);
		commitAndBeginNewTransaction();

		List<Sak> sakerSomSkalVaereOppdaterte = sakRepository.findAllById(List.of(relevantSak.getSakId(), sakMedAnnetTema.getSakId()));
		List<Sak> sakerSomIkkeSkalVaereOppdaterte = sakRepository.findAllById(List.of(sakUtenAktoerId.getSakId(), sakMedFeilSaksstatus.getSakId()));

		assertThat(sakerSomSkalVaereOppdaterte).allSatisfy(this::assertSakErOppdatert);
		assertThat(sakerSomIkkeSkalVaereOppdaterte).allSatisfy(this::assertSakIkkeErOppdatert);
	}

	@Test
	void skalReturnereFnrForPersonerSomHarVaertDoedeITiAar() {
		var sakMedFeilTema = createBaseSak().tema(TEMA_BIL).brukerId(BRUKERID2_FNR).doedsdato(DOEDSDATO_OVER_10_AAR).build();
		var sakUtenDoedsdato = createBaseSak().brukerId(BRUKERID2_FNR).build();
		var sakMedForNyDoedsdato = createBaseSak().brukerId(BRUKERID2_FNR).doedsdato(DOEDSDATO_UNDER_10_AAR).build();
		var sakMedKassasjonsstatus = createBaseSak().brukerId(BRUKERID2_FNR).kassasjonsstatus(KASSERT).doedsdato(DOEDSDATO_OVER_10_AAR).build();
		var relevantSak = createBaseSak().brukerId(BRUKERID_FNR).doedsdato(DOEDSDATO_OVER_10_AAR).build();
		var relevantSakMedSammeAktoerId = createBaseSak().brukerId(BRUKERID_FNR).doedsdato(DOEDSDATO_OVER_10_AAR).build();
		sakRepository.saveAll(List.of(sakMedFeilTema, sakUtenDoedsdato, sakMedForNyDoedsdato, sakMedKassasjonsstatus, relevantSak, relevantSakMedSammeAktoerId));

		Set<String> avdoedePersoner = sakRepository.findDistinctFnrHvorPersonErDoedIMerEnnMaaneder(TEMA_AAP, 120);

		assertThat(avdoedePersoner)
				.hasSize(1)
				.contains(BRUKERID_FNR);
	}

	@Test
	void skalFinneUkasserteSakerForBruker() {
		var sakMedFeilTema = createBaseSak().tema(TEMA_BIL).brukerId(BRUKERID_FNR).build();
		var sakMedFeilBruker = createBaseSak().brukerId(BRUKERID2_FNR).build();
		var sakMedKassasjonsstatus = createBaseSak().brukerId(BRUKERID_FNR).kassasjonsstatus(KASSERT).build();
		var relevantSak = createBaseSak().brukerId(BRUKERID_FNR).build();
		sakRepository.saveAll(List.of(sakMedFeilTema, sakMedFeilBruker, sakMedKassasjonsstatus, relevantSak));

		Set<Sak> ukasserteSaker = sakRepository.finnUkasserteSakerForBrukere(List.of(BRUKERID_FNR), TEMA_AAP);

		assertThat(ukasserteSaker)
				.hasSize(1)
				.extracting(Sak::getSakId)
				.containsExactly(relevantSak.getSakId());
	}

	private Sak.SakBuilder createBaseSak() {
		return Sak.builder()
				.sakId(sakIdCounter++)
				.tema(TEMA_AAP);
	}

	private void assertSakErOppdatert(Sak sak) {
		assertThat(sak.getBrukerIdType()).isEqualTo(BRUKERIDTYPE);
		assertThat(sak.getBrukerId()).isEqualTo(BRUKERID_FNR);
		assertThat(sak.getDoedsdato()).isEqualTo(DOEDSDATO);
		assertThat(sak.getEndretAv()).isEqualTo(ENDRET_AV);
		assertThat(sak.getDatoEndret()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));
	}

	private void assertSakIkkeErOppdatert(Sak sak) {
		assertThat(sak)
				.extracting("brukerIdType", "brukerId", "doedsdato", "endretAv", "datoEndret")
				.containsOnlyNulls();
	}

	private void commitAndBeginNewTransaction() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();
	}

}