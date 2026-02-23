package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.domain.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;

import java.time.LocalDate;

import static java.util.Objects.isNull;

public class DoedsfallhendelseMapper {

	public static Doedsfallhendelse mapPersonhendelseTilDoedsfall(Personhendelse personhendelse) {
		return Doedsfallhendelse.builder()
				.hendelseId(personhendelse.getHendelseId().toString())
				.doedsdato(mapDoedsdato(personhendelse))
				.endringstype(Endringstype.valueOf(personhendelse.getEndringstype().toString()))
				.personidenter(personhendelse.getPersonidenter().stream().map(Object::toString).toList())
				.build();
	}

	private static LocalDate mapDoedsdato(Personhendelse personhendelse) {
		Doedsfall doedsfall = personhendelse.getDoedsfall();
		return isNull(doedsfall) ? null : doedsfall.getDoedsdato();
	}

}