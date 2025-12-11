package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.domain.Fagomraade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static no.nav.dokarkivpleie.FagomraadeValidator.erFagomraadeUgyldig;
import static org.assertj.core.api.Assertions.assertThat;

class FagomraadeValidatorTest {

	private static final String USTOETTET_BEVARINGSTID = "100_AAR_ETTER_AVSLUTTET_SAK";

	@ParameterizedTest
	@MethodSource
	void fagomraadeErGyldig(Fagomraade fagomraade) {
		assertThat(erFagomraadeUgyldig(fagomraade)).isFalse();
	}

	static Stream<Arguments> fagomraadeErGyldig() {
		return Stream.of(
				Arguments.of(new Fagomraade("AAP", "10_AAR_ETTER_BRUKERS_DOED", true)),
				Arguments.of(new Fagomraade("BIL", "10_AAR_ETTER_BRUKERS_DOED", true)),
				Arguments.of(new Fagomraade("ENF", "10_AAR_ETTER_BRUKERS_DOED", true)),
				Arguments.of(new Fagomraade("TRY", "25_AAR_ETTER_BRUKERS_DOED", false)),
				Arguments.of(new Fagomraade("UFM", "25_AAR_ETTER_BRUKERS_DOED", true)),
				Arguments.of(new Fagomraade("UFO", "25_AAR_ETTER_BRUKERS_DOED", true)),
				Arguments.of(new Fagomraade("YRK", "25_AAR_ETTER_BRUKERS_DOED", true))
		);
	}

	@Test
	void fagomraadeErUgyldigHvisFagomraadeErNull() {
		assertThat(erFagomraadeUgyldig(null)).isTrue();
	}

	@Test
	void fagomraadeErUgyldigHvisBevaringstidForFagomraadeErNull() {
		var fagomraadeMedBevaringstidNull = new Fagomraade("PEN", null, true);

		assertThat(erFagomraadeUgyldig(fagomraadeMedBevaringstidNull)).isTrue();
	}

	@Test
	void fagomraadeErUgyldigForBevaringstiderSomIkkeErStoettet() {
		var fagomraadeMedBevaringstidSomIkkeErStoettet = new Fagomraade("PEN", USTOETTET_BEVARINGSTID, true);

		assertThat(erFagomraadeUgyldig(fagomraadeMedBevaringstidSomIkkeErStoettet)).isTrue();
	}

	@Test
	void fagomraadeErUgyldigHvisAvleverMedDokErNull() {
		var fagomraadeMedAvleverMedDokErNull = new Fagomraade("FIP", "10_AAR_ETTER_BRUKERS_DOED", null);

		assertThat(erFagomraadeUgyldig(fagomraadeMedAvleverMedDokErNull)).isTrue();
	}

}