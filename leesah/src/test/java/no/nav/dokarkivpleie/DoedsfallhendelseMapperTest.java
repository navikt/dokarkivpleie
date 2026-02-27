package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.domain.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static no.nav.dokarkivpleie.DoedsfallhendelseMapper.mapPersonhendelseTilDoedsfall;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.ANNULLERT;
import static org.assertj.core.api.Assertions.assertThat;

class DoedsfallhendelseMapperTest {

	private static final String HENDELSE_ID = "123";
	private static final List<CharSequence> PERSONIDENTER = List.of("2556016505784", "1556016505799");
	private static final String MASTER = "PDL";
	private static final String OPPLYSNINGSTYPE = "DOEDSFALL_V1";
	private static final LocalDate DOEDSDATO = LocalDate.of(2024, 1, 1);

	@ParameterizedTest
	@EnumSource(value = no.nav.person.pdl.leesah.Endringstype.class, names = {"OPPRETTET", "KORRIGERT", "OPPHOERT"})
	void skalMappeDoedsfallhendelse(no.nav.person.pdl.leesah.Endringstype endringstype) {
		Personhendelse personhendelse = lagPersonhendelse(endringstype, DOEDSDATO);

		Doedsfallhendelse doedsfallhendelse = mapPersonhendelseTilDoedsfall(personhendelse);

		assertThat(doedsfallhendelse.hendelseId()).isEqualTo(HENDELSE_ID);
		assertThat(doedsfallhendelse.personidenter()).isEqualTo(PERSONIDENTER);
		assertThat(doedsfallhendelse.endringstype()).isEqualTo(Endringstype.valueOf(endringstype.name()));
		assertThat(doedsfallhendelse.doedsdato()).isEqualTo(DOEDSDATO);
	}

	@Test
	void skalMappeDoedsfallhendelseForEndringstypeAnnullert() {
		Personhendelse personhendelse = lagPersonhendelse(no.nav.person.pdl.leesah.Endringstype.ANNULLERT, null);

		Doedsfallhendelse doedsfallhendelse = mapPersonhendelseTilDoedsfall(personhendelse);

		assertThat(doedsfallhendelse.hendelseId()).isEqualTo(HENDELSE_ID);
		assertThat(doedsfallhendelse.personidenter()).isEqualTo(PERSONIDENTER);
		assertThat(doedsfallhendelse.endringstype()).isEqualTo(ANNULLERT);
		assertThat(doedsfallhendelse.doedsdato()).isNull();
	}

	private Personhendelse lagPersonhendelse(no.nav.person.pdl.leesah.Endringstype endringstype, LocalDate doedsdato) {
		Doedsfall doedsfall = Doedsfall.newBuilder()
				.setDoedsdato(doedsdato)
				.build();

		Personhendelse personhendelse = new Personhendelse();
		personhendelse.setHendelseId(HENDELSE_ID);
		personhendelse.setPersonidenter(PERSONIDENTER);
		personhendelse.setMaster(MASTER);
		personhendelse.setOpprettet(Instant.now());
		personhendelse.setOpplysningstype(OPPLYSNINGSTYPE);
		personhendelse.setEndringstype(endringstype);
		personhendelse.setDoedsfall(doedsfall);

		return personhendelse;
	}
}