package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MerkSakerBevaringstidPassertScheduler {

	// Kun bevaringstidene 10_AAR_ETTER_BRUKERS_DOED og 25_AAR_ETTER_BRUKERS_DOED er støttet i første iterasjon av jobben
	private static final List<String> TEMA_MED_STOETTEDE_BEVARINGSTIDER = List.of("AAP", "BIL", "ENF", "TRY", "UFM", "UFO", "YRK");

	private final MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService;

	MerkSakerBevaringstidPassertScheduler(MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService) {
		this.merkSakerBevaringstidPassertService = merkSakerBevaringstidPassertService;
	}

	@Scheduled(initialDelay = 10000L)
	public void kjoerPeriodiskJobb() {
		log.info("Starter periodisk jobb for å markere saker der bevaringstid har passert for tema={}.", TEMA_MED_STOETTEDE_BEVARINGSTIDER);

		for (String tema : TEMA_MED_STOETTEDE_BEVARINGSTIDER) {
			merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(tema);
		}

		log.info("Periodisk jobb for å markere saker der bevaringstid har passert for tema={} er avsluttet.", TEMA_MED_STOETTEDE_BEVARINGSTIDER);
	}
}
