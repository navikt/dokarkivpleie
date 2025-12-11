package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.domain.Fagomraade;

import java.util.List;

@Slf4j
public class FagomraadeValidator {

	public static final List<String> STOETTEDE_BEVARINGSTIDER = List.of("10_AAR_ETTER_BRUKERS_DOED", "25_AAR_ETTER_BRUKERS_DOED");

	public static boolean erFagomraadeUgyldig(Fagomraade fagomraade) {
		if (fagomraade == null) {
			log.warn("Fagomraade er null");
			return true;
		}

		if (fagomraade.getBevaringstid() == null) {
			log.warn("Fagomraade={} sin bevaringstid er ikke satt.", fagomraade.getKode());
			return true;
		}

		if (bevaringstidErIkkeStoettet(fagomraade.getBevaringstid())) {
			log.warn("Første versjon av MerkSakerBevaringstidPassert-jobben behandler kun fagområder med bevaringstid={}. Mottok={}.", STOETTEDE_BEVARINGSTIDER, fagomraade.getBevaringstid());
			return true;
		}

		if (fagomraade.getAvleverMedDok() == null) {
			log.warn("Avlevering med eller uten dokumenter er ikke satt for fagomraade={}.", fagomraade.getKode());
			return true;
		}

		return false;
	}

	private static boolean bevaringstidErIkkeStoettet(String bevaringstid) {
		return !STOETTEDE_BEVARINGSTIDER.contains(bevaringstid);
	}

}