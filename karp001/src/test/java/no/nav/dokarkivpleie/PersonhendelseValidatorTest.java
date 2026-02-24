package no.nav.dokarkivpleie;

import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static no.nav.dokarkivpleie.PersonhendelseValidator.validerPersonhendelse;
import static no.nav.person.pdl.leesah.Endringstype.ANNULLERT;
import static no.nav.person.pdl.leesah.Endringstype.OPPHOERT;
import static no.nav.person.pdl.leesah.Endringstype.OPPRETTET;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PersonhendelseValidatorTest {

	private static final String HENDELSE_ID = "test-hendelse-123";
	private static final String PERSONIDENT = "12345678901";
	private static final String MASTER = "PDL";
	private static final String OPPLYSNINGSTYPE = "DOEDSFALL_V1";
	private static final LocalDate DOEDSDATO = LocalDate.of(2024, 1, 1);

	@ParameterizedTest
	@EnumSource(value = Endringstype.class, names = {"OPPRETTET", "KORRIGERT", "ANNULLERT"})
	void skalValidereOkNaarPersonhendelseErGyldig(Endringstype endringstype) {
		Personhendelse personhendelse = opprettPersonhendelse(DOEDSDATO, List.of(PERSONIDENT));
		personhendelse.setEndringstype(endringstype);

		assertThatNoException().isThrownBy(() -> validerPersonhendelse(personhendelse));
	}

	@Test
	void skalValidereOkMedFlerePersonidenter() {
		Personhendelse personhendelse = opprettPersonhendelse(DOEDSDATO, List.of("12345678901", "98765432109", "11223344556"));

		assertThatNoException().isThrownBy(() -> validerPersonhendelse(personhendelse));
	}

	@Test
	void skalKasteValideringFeiletFunctionalExceptionForFeilEndringstype() {
		Personhendelse personhendelse = opprettPersonhendelse(DOEDSDATO, List.of(PERSONIDENT));
		personhendelse.setEndringstype(OPPHOERT);

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerPersonhendelse(personhendelse))
				.withMessage("Hendelse med opplysningstype=%s og hendelseId=%s har endringstype=%s. Ignorerer hendelse.".formatted(OPPLYSNINGSTYPE, HENDELSE_ID, OPPHOERT.toString()));
	}

	@Test
	void skalKasteValideringFeiletFunctionalExceptionNaarPersonidenterErTom() {
		Personhendelse personhendelse = opprettPersonhendelse(DOEDSDATO, emptyList());

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerPersonhendelse(personhendelse))
				.withMessage("Hendelse med opplysningstype=%s og hendelseId=%s har ingen personidenter. Ignorerer hendelse.".formatted(OPPLYSNINGSTYPE, HENDELSE_ID));
	}

	@ParameterizedTest
	@EnumSource(value = Endringstype.class, names = {"OPPRETTET", "KORRIGERT"})
	void skalKasteValideringFeiletFunctionalExceptionNaarDoedsfallErNull(Endringstype endringstype) {
		Personhendelse personhendelse = opprettPersonhendelse(null, List.of(PERSONIDENT));
		personhendelse.setDoedsfall(null);
		personhendelse.setEndringstype(endringstype);

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerPersonhendelse(personhendelse))
				.withMessage("Hendelse med opplysningstype=%s og hendelseId=%s har dødsfall null. Ignorerer hendelse.".formatted(OPPLYSNINGSTYPE, HENDELSE_ID));
	}

	@Test
	void skalIkkeKasteValideringFeiletFunctionalExceptionNaarDoedsfallErNullForEndringstypeAnnullert() {
		Personhendelse personhendelse = opprettPersonhendelse(null, List.of(PERSONIDENT));
		personhendelse.setEndringstype(ANNULLERT);
		personhendelse.setDoedsfall(null);

		assertThatNoException().isThrownBy(() -> validerPersonhendelse(personhendelse));
	}

	@Test
	void skalIkkeKasteValideringFeiletFunctionalExceptionNaarDoedsdatoErNullForEndringstypeAnnullert() {
		Personhendelse personhendelse = opprettPersonhendelse(null, List.of(PERSONIDENT));
		personhendelse.setEndringstype(ANNULLERT);

		assertThatNoException().isThrownBy(() -> validerPersonhendelse(personhendelse));
	}

	@ParameterizedTest
	@EnumSource(value = Endringstype.class, names = {"OPPRETTET", "KORRIGERT"})
	void skalKasteValideringFeiletFunctionalExceptionNaarDoedsdatoErNullForEndringstyperOpprettetOgKorrigert(Endringstype endringstype) {
		Personhendelse personhendelse = opprettPersonhendelse(null, List.of(PERSONIDENT));
		personhendelse.setEndringstype(endringstype);

		assertThatExceptionOfType(ValideringFeiletFunctionalException.class)
				.isThrownBy(() -> validerPersonhendelse(personhendelse))
				.withMessage("Hendelse med opplysningstype=%s og hendelseId=%s mangler dødsdato. Ignorerer hendelse.".formatted(OPPLYSNINGSTYPE, HENDELSE_ID));
	}

	private Personhendelse opprettPersonhendelse(LocalDate doedsdato, List<String> personidenter) {
		return Personhendelse.newBuilder()
				.setHendelseId(HENDELSE_ID)
				.setOpplysningstype(OPPLYSNINGSTYPE)
				.setPersonidenter(personidenter != null ? personidenter.stream().map(s -> (CharSequence) s).toList() : null)
				.setDoedsfall(Doedsfall.newBuilder().setDoedsdato(doedsdato).build())
				.setMaster(MASTER)
				.setOpprettet(Instant.now())
				.setEndringstype(OPPRETTET)
				.build();
	}

}