package no.nav.dokarkivpleie;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.repository.FagomraadeRepository;
import no.nav.dokarkivpleie.repository.SakRepository;
import no.nav.dokarkivpleie.service.AdministrativEnhetService;
import no.nav.dokarkivpleie.service.ArkivsakService;
import no.nav.dokarkivpleie.service.FnrOgDoedsdatoService;
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
	private final FnrOgDoedsdatoService fnrOgDoedsdatoService;
	private final AdministrativEnhetService administrativEnhetService;
	private final SakRepository sakRepository;
	private final ArkivsakService arkivsakService;

	MerkSakerBevaringstidPassertService(FagomraadeRepository fagomraadeRepository,
										SakRepository sakRepository,
										FnrOgDoedsdatoService fnrOgDoedsdatoService,
										AdministrativEnhetService administrativEnhetService, ArkivsakService arkivsakService) {
		this.fagomraadeRepository = fagomraadeRepository;
		this.sakRepository = sakRepository;
		this.fnrOgDoedsdatoService = fnrOgDoedsdatoService;
		this.administrativEnhetService = administrativEnhetService;
		this.arkivsakService = arkivsakService;
	}

	public void merkSakerBevaringstidPassert(@NonNull String tema) {
		administrativEnhetService.hentAdministrativeEnheterFraDatavarehus();

		log.info("Skal markere saker der bevaringstid har passert for tema={}.", tema);

		Fagomraade fagomraade = fagomraadeRepository.findFagomraadeByKode(tema);
		if (erFagomraadeUgyldig(fagomraade)) {
			log.warn("Avslutter markering av saker der bevaringstid har passert for ugyldig tema={}.", tema);
			return;
		}

		int bevaringstidAntallMaaneder = settBevaringstidIMaaneder(fagomraade);

		fnrOgDoedsdatoService.oppdaterFnrOgDoedsdatoForAvdoedePersoner(fagomraade);

		Set<String> fnrSomHarVaertDoedeLengerEnnBevaringstid = sakRepository.findDistinctFnrHvorPersonErDoedIMerEnnMaaneder(tema, bevaringstidAntallMaaneder);
		log.info("Har hentet {} døde personer for merking av saker der bevaringstid er passert", fnrSomHarVaertDoedeLengerEnnBevaringstid.size());

		List<List<String>> partisjonertDoedePersonerList = Lists.partition(fnrSomHarVaertDoedeLengerEnnBevaringstid.stream().toList(), 200);

		partisjonertDoedePersonerList.forEach(doedePersonerPartisjon -> arkivsakService.kasserSakerForDoedePersoner(doedePersonerPartisjon, tema, fagomraade));

	}

	private int settBevaringstidIMaaneder(Fagomraade fagomraade) {
		if (BEVARINGSTID_10_AAR_ETTER_BRUKERS_DOED.equals(fagomraade.getBevaringstid())) {
			return 120;
		} else { // 25 år
			return 300;
		}
	}


}