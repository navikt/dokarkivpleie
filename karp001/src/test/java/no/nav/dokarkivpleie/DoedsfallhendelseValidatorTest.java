package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.Doedsfallhendelse.DoedsfallhendelseBuilder;
import no.nav.dokarkivpleie.domain.leesah.Endringstype;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.dokarkivpleie.DoedsfallhendelseValidator.validerDoedsfall;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.ANNULLERT;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.OPPHOERT;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.OPPRETTET;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class DoedsfallhendelseValidatorTest {

	private static final String HENDELSE_ID = "test-hendelse-123";
	private static final String PERSONIDENT = "12345678901";
	private static final LocalDate DOEDSDATO = LocalDate.of(2024, 1, 1);

	@ParameterizedTest
	@EnumSource(value = Endringstype.class, names = {"OPPRETTET", "KORRIGERT"})
	void skalValidereOkForGyldigDoedsfallhendelseMedEndringstypeOpprettetEllerKorrigert(Endringstype endringstype) {
		DoedsfallhendelseBuilder doedsfall = opprettPersonhendelse(DOEDSDATO, List.of(PERSONIDENT));
		doedsfall.endringstype(endringstype);

		assertThatNoException().isThrownBy(() -> validerDoedsfall(doedsfall.build()));
	}

	@Test
	void skalValidereOkForGyldigDoedsfallhendelseMedEndringstypeAnnullertOgNullDoedsdato() {
		DoedsfallhendelseBuilder doedsfall = opprettPersonhendelse(null, List.of(PERSONIDENT));
		doedsfall.endringstype(ANNULLERT);

		assertThatNoException().isThrownBy(() -> validerDoedsfall(doedsfall.build()));
	}

	@Test
	void skalKasteValideringFeiletFunctionalExceptionForFeilEndringstype() {
		DoedsfallhendelseBuilder doedsfall = opprettPersonhendelse(null, List.of(PERSONIDENT));
		doedsfall.endringstype(OPPHOERT);

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerDoedsfall(doedsfall.build()))
				.withMessage("Doedsfall-hendelse med hendelseId=%s har endringstype=%s. Ignorerer hendelse.", HENDELSE_ID, OPPHOERT.toString());
	}

	@ParameterizedTest
	@EnumSource(value = Endringstype.class, names = {"OPPRETTET", "KORRIGERT", "ANNULLERT"})
	void skalValidereOkMedFlerePersonidenter(Endringstype endringstype) {
		DoedsfallhendelseBuilder doedsfall = opprettPersonhendelse(DOEDSDATO, List.of("12345678901", "98765432109", "11223344556"));
		doedsfall.endringstype(endringstype);

		assertThatNoException().isThrownBy(() -> validerDoedsfall(doedsfall.build()));
	}

	@Test
	void skalKasteValideringFeiletFunctionalExceptionNaarPersonidenterErTom() {
		DoedsfallhendelseBuilder doedsfall = opprettPersonhendelse(DOEDSDATO, emptyList());

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerDoedsfall(doedsfall.build()))
				.withMessage("Doedsfall-hendelse med hendelseId=%s har ingen personidenter. Ignorerer hendelse.", HENDELSE_ID);
	}

	@ParameterizedTest
	@EnumSource(value = Endringstype.class, names = {"OPPRETTET", "KORRIGERT"})
	void skalKasteValideringFeiletFunctionalExceptionNaarDoedsdatoErNull(Endringstype endringstype) {
		DoedsfallhendelseBuilder doedsfall = opprettPersonhendelse(null, List.of(PERSONIDENT));
		doedsfall.endringstype(endringstype);

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerDoedsfall(doedsfall.build()))
				.withMessage("Doedsfall-hendelse med hendelseId=%s og endringstype=%s mangler dødsdato. Ignorerer hendelse.", HENDELSE_ID, endringstype);
	}

	private DoedsfallhendelseBuilder opprettPersonhendelse(LocalDate doedsdato, List<String> personidenter) {
		return Doedsfallhendelse.builder()
				.hendelseId(HENDELSE_ID)
				.doedsdato(doedsdato)
				.endringstype(OPPRETTET)
				.personidenter(personidenter);
		
	}

}