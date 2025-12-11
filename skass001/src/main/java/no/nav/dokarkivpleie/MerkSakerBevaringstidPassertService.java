package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.repository.FagomraadeRepository;
import no.nav.dokarkivpleie.repository.SakRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.dokarkivpleie.FagomraadeValidator.erFagomraadeUgyldig;

@Slf4j
@Service
public class MerkSakerBevaringstidPassertService {

	private static final List<String> BEVARINGSTIDER_10_ELLER_25_AAR_ETTER_BRUKERS_DOED = List.of("10_AAR_ETTER_BRUKERS_DOED", "25_AAR_ETTER_BRUKERS_DOED");

	private final FagomraadeRepository fagomraadeRepository;
	private final SakRepository sakRepository;

	MerkSakerBevaringstidPassertService(FagomraadeRepository fagomraadeRepository,
										SakRepository sakRepository) {
		this.fagomraadeRepository = fagomraadeRepository;
		this.sakRepository = sakRepository;
	}

	public void merkSakerBevaringstidPassert(String tema) {
		log.info("Skal markere saker der bevaringstid har passert for tema={}.", tema);

		Fagomraade fagomraade = fagomraadeRepository.findFagomraadeByKode(tema);
		if (erFagomraadeUgyldig(fagomraade)) {
			log.info("Avslutter markering av saker der bevaringstid har passert for tema={}.", tema);
			return;
		}

		// 2. Oppdater fødselsnummer og dødsdato
		if (BEVARINGSTIDER_10_ELLER_25_AAR_ETTER_BRUKERS_DOED.contains(fagomraade.getBevaringstid())) {
			log.info("Skal hente aktoerIder for oppdatering av fødselsnummer og dødsdato");
			List<String> aktoerIder = sakRepository.finnAktoeriderDerFoedselsnummerOgDoedsdatoSkalBliOppdatert(fagomraade.getKode());
			log.info("Har hentet {} aktoerIder for oppdatering av fødselsnummer og dødsdato", aktoerIder.size());
		}

		log.info("Skal merke saker der bevaringstid har passert for tema={} for bevaringstid={}.", tema, fagomraade.getBevaringstid());


	}
}