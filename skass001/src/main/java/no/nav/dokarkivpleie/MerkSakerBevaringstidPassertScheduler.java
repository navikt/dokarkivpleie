package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.service.AdministrativEnhetService;
import no.nav.dokarkivpleie.slack.SlackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MerkSakerBevaringstidPassertScheduler {

	// Kun bevaringstidene 10_AAR_ETTER_BRUKERS_DOED og 25_AAR_ETTER_BRUKERS_DOED er støttet i første iterasjon av jobben
	private static final List<String> TEMA_MED_STOETTEDE_BEVARINGSTIDER = List.of("AAP", "BIL", "ENF", "TRY", "UFM", "UFO", "YRK");

	private final MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService;
	private final AdministrativEnhetService administrativEnhetService;
	private final SlackService slackService;

	MerkSakerBevaringstidPassertScheduler(MerkSakerBevaringstidPassertService merkSakerBevaringstidPassertService,
										  AdministrativEnhetService administrativEnhetService,
										  SlackService slackService) {
		this.merkSakerBevaringstidPassertService = merkSakerBevaringstidPassertService;
		this.administrativEnhetService = administrativEnhetService;
		this.slackService = slackService;
	}

	@Scheduled(initialDelay = 10000L)
	public void kjoerPeriodiskJobb() {
		log.info("Starter Skass001 for å markere saker der bevaringstid har passert for tema={}.", TEMA_MED_STOETTEDE_BEVARINGSTIDER);

		try {
			administrativEnhetService.hentAdministrativeEnheterFraDatavarehus();
			for (String tema : TEMA_MED_STOETTEDE_BEVARINGSTIDER) {
				merkSakerBevaringstidPassertService.merkSakerBevaringstidPassert(tema);
			}
		} catch (Exception e) {
			log.error("Skass001 feilet med exception: ", e);
			slackService.sendMelding("Skass001 har feilet. Avslutter dagens kjøring - dette må undersøkes.");
		}

		log.info("Periodisk jobb for å markere saker der bevaringstid har passert for tema={} er avsluttet.", TEMA_MED_STOETTEDE_BEVARINGSTIDER);
	}
}
