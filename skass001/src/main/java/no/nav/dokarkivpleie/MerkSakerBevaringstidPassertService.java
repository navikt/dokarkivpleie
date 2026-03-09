package no.nav.dokarkivpleie;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.repository.FagomraadeRepository;
import no.nav.dokarkivpleie.repository.SakRepository;
import no.nav.dokarkivpleie.service.ArkivsakService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static no.nav.dokarkivpleie.FagomraadeValidator.BEVARINGSTID_10_AAR_ETTER_BRUKERS_DOED;
import static no.nav.dokarkivpleie.FagomraadeValidator.erFagomraadeUgyldig;

@Slf4j
@Service
public class MerkSakerBevaringstidPassertService {

	public static final String DOKARKIVPLEIE = "dokarkivpleie";
	public static final String MERK_SAKER_BEVARINGSTID_PASSERT = "MerkSakerBevaringstidPassert";

	private final FagomraadeRepository fagomraadeRepository;
	private final SakRepository sakRepository;
	private final ArkivsakService arkivsakService;

	MerkSakerBevaringstidPassertService(FagomraadeRepository fagomraadeRepository,
										SakRepository sakRepository,
										ArkivsakService arkivsakService) {
		this.fagomraadeRepository = fagomraadeRepository;
		this.sakRepository = sakRepository;
		this.arkivsakService = arkivsakService;
	}

	public void merkSakerBevaringstidPassert(@NonNull String tema) {
		log.info("Skal markere saker der bevaringstid har passert for tema={}.", tema);

		Fagomraade fagomraade = fagomraadeRepository.findFagomraadeByKode(tema);
		if (erFagomraadeUgyldig(fagomraade)) {
			log.error("Avslutter markering av saker der bevaringstid har passert for ugyldig tema={}.", tema);
			return;
		}

		Set<String> fnrTilPersonerSomHarVaertDoedeLengerEnnBevaringstid = sakRepository.findDistinctFnrHvorPersonErDoedIMerEnnMaaneder(tema, hentBevaringstidForFagomraade(fagomraade));
		log.info("Har hentet {} døde personer for merking av saker der bevaringstid er passert", fnrTilPersonerSomHarVaertDoedeLengerEnnBevaringstid.size());

		List<List<String>> partisjonertDoedePersonerList = Lists.partition(fnrTilPersonerSomHarVaertDoedeLengerEnnBevaringstid.stream().toList(), 200);
		partisjonertDoedePersonerList.forEach(doedePersonerPartisjon -> arkivsakService.kasserSakerForDoedePersoner(doedePersonerPartisjon, tema, fagomraade));
	}

	private int hentBevaringstidForFagomraade(Fagomraade fagomraade) {
		if (BEVARINGSTID_10_AAR_ETTER_BRUKERS_DOED.equals(fagomraade.getBevaringstid())) {
			return 120;
		} else { // 25 år
			return 300;
		}
	}

}