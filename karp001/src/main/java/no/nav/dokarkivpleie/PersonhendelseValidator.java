package no.nav.dokarkivpleie;

import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;

import java.util.EnumSet;

import static java.lang.String.format;
import static no.nav.person.pdl.leesah.Endringstype.ANNULLERT;
import static no.nav.person.pdl.leesah.Endringstype.KORRIGERT;
import static no.nav.person.pdl.leesah.Endringstype.OPPRETTET;

public class PersonhendelseValidator {

	private static final EnumSet<Endringstype> RELEVANTE_ENDRINGSTYPER = EnumSet.of(OPPRETTET, KORRIGERT, ANNULLERT);
	private static final EnumSet<Endringstype> ENDRINGSTYPER_SOM_SKAL_HA_DOEDSDATO = EnumSet.of(OPPRETTET, KORRIGERT);

	public static void validerPersonhendelse(Personhendelse personhendelse) {
		if (!RELEVANTE_ENDRINGSTYPER.contains(personhendelse.getEndringstype())) {
			throw new ValideringFeiletFunctionalException(format("Hendelse med opplysningstype=%s og hendelseId=%s har endringstype=%s. Ignorerer hendelse.", personhendelse.getOpplysningstype(), personhendelse.getHendelseId(), personhendelse.getEndringstype().toString()));
		}

		if (personhendelse.getPersonidenter().isEmpty()) {
			throw new ValideringFeiletFunctionalException(format("Hendelse med opplysningstype=%s og hendelseId=%s har ingen personidenter. Ignorerer hendelse.", personhendelse.getOpplysningstype(), personhendelse.getHendelseId()));
		}

		if (ENDRINGSTYPER_SOM_SKAL_HA_DOEDSDATO.contains(personhendelse.getEndringstype())) {
			if (personhendelse.getDoedsfall() == null) {
				throw new ValideringFeiletFunctionalException(format("Hendelse med opplysningstype=%s og hendelseId=%s har dødsfall null. Ignorerer hendelse.", personhendelse.getOpplysningstype(), personhendelse.getHendelseId()));
			}

			if (personhendelse.getDoedsfall().getDoedsdato() == null) {
				throw new ValideringFeiletFunctionalException(format("Hendelse med opplysningstype=%s og hendelseId=%s mangler dødsdato. Ignorerer hendelse.", personhendelse.getOpplysningstype(), personhendelse.getHendelseId()));
			}
		}

	}
}
