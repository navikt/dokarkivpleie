package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.config.RepositoryConfig;
import no.nav.dokarkivpleie.domain.Sak;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;

import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.KASSERT;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {RepositoryConfig.class})
class SakRepositoryTest {

	private static final String AKTOER_ID1 = "3345247044473";
	private static final String AKTOER_ID2 = "0565039150168";
	private static final String AKTOER_ID3 = "0762894254017";
	private static final String RELEVANT_TEMA = "AAP";
	private static final String IRRELEVANT_TEMA = "BIL";

	@Autowired
	SakRepository sakRepository;

	@Test
	void skalFinneAktoeriderSomSkalFaaOppdatertFoedselsnummerOgDoedsdato() {
		leggInnSaker();

		List<String> saker = sakRepository.finnAktoeriderDerFoedselsnummerOgDoedsdatoSkalBliOppdatert("AAP");

		assertThat(saker)
				.isNotNull()
				.hasSize(2)
				.contains(AKTOER_ID1, AKTOER_ID2);
	}

	private void leggInnSaker() {
		var sak = Sak.builder()
				.sakId(100L)
				.aktoerId(AKTOER_ID1)
				.tema(RELEVANT_TEMA)
				.build();
		var sakMedSammeAktoerId = Sak.builder()
				.sakId(101L)
				.aktoerId(AKTOER_ID1)
				.tema(RELEVANT_TEMA)
				.build();
		var sakUtenAktoerId = Sak.builder()
				.sakId(102L)
				.tema(RELEVANT_TEMA)
				.build();
		var sakMedKassasjonsstatus = Sak.builder()
				.sakId(103L)
				.aktoerId(AKTOER_ID3)
				.tema(RELEVANT_TEMA)
				.kassasjonsstatus(KASSERT)
				.build();
		var sakMedDoedsdato = Sak.builder()
				.sakId(104L)
				.aktoerId(AKTOER_ID3)
				.tema(RELEVANT_TEMA)
				.doedsdato(LocalDate.now().minusWeeks(1))
				.build();
		var sakMedFeilTema = Sak.builder()
				.sakId(105L)
				.aktoerId(AKTOER_ID3)
				.tema(IRRELEVANT_TEMA)
				.build();
		var sakMedAnnenAktoerId = Sak.builder()
				.sakId(101L)
				.aktoerId(AKTOER_ID2)
				.tema(RELEVANT_TEMA)
				.build();

		sakRepository.saveAll(List.of(sak, sakMedSammeAktoerId, sakUtenAktoerId, sakMedKassasjonsstatus, sakMedDoedsdato, sakMedFeilTema, sakMedAnnenAktoerId));
	}


}