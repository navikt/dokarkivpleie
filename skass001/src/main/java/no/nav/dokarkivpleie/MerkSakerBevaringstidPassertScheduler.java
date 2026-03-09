package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusFunctionalException;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusTechnicalException;
import no.nav.dokarkivpleie.service.AdministrativEnhetService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MerkSakerBevaringstidPassertScheduler {

	// Kun bevaringstidene 10_AAR_ETTER_BRUKERS_DOED og 25_AAR_ETTER_BRUKERS_DOED er støttet i første iterasjon av jobben
	private static final List<String> TEMA_MED_STOETTEDE_BEVARINGSTIDER = List.of("AAP", "BIL", "ENF", "TRY", "UFM", "UFO", "YRK");

	private final MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService;
	private final AdministrativEnhetService administrativEnhetService;

	MerkSakerBevaringstidPassertScheduler(MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService,
										  AdministrativEnhetService administrativEnhetService) {
		this.merkSakerBevaringstidPassertService = merkSakerBevaringstidPassertService;
		this.administrativEnhetService = administrativEnhetService;
	}

	//TODO: Skru på scheduled når vi er ferdige med karp001
	//@Scheduled(initialDelay = 10000L)
	public void kjoerPeriodiskJobb() {
		log.info("Starter periodisk jobb for å markere saker der bevaringstid har passert for tema={}.", TEMA_MED_STOETTEDE_BEVARINGSTIDER);

		try {
			administrativEnhetService.hentAdministrativeEnheterFraDatavarehus();
		} catch (DatavarehusFunctionalException | DatavarehusTechnicalException e) {
			log.error("Merksakerbevaringstidpassert feilet mot dvh med exception: ", e);
			return;
		}

		for (String tema : TEMA_MED_STOETTEDE_BEVARINGSTIDER) {
			merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(tema);
		}

		log.info("Periodisk jobb for å markere saker der bevaringstid har passert for tema={} er avsluttet.", TEMA_MED_STOETTEDE_BEVARINGSTIDER);
	}
}
