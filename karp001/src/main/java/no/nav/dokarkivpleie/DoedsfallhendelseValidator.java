package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.domain.leesah.Endringstype;

import java.util.EnumSet;

import static java.lang.String.format;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.ANNULLERT;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.KORRIGERT;
import static no.nav.dokarkivpleie.domain.leesah.Endringstype.OPPRETTET;

public class DoedsfallhendelseValidator {

	private static final EnumSet<Endringstype> RELEVANTE_ENDRINGSTYPER = EnumSet.of(OPPRETTET, KORRIGERT, ANNULLERT);
	private static final EnumSet<Endringstype> ENDRINGSTYPER_SOM_SKAL_HA_DOEDSDATO = EnumSet.of(OPPRETTET, KORRIGERT);

	public static void validerDoedsfall(Doedsfallhendelse doedsfall) {
		if (!RELEVANTE_ENDRINGSTYPER.contains(doedsfall.endringstype())) {
			throw new ValideringFeiletFunctionalException(format("Doedsfall-hendelse med hendelseId=%s har endringstype=%s. Ignorerer hendelse.", doedsfall.hendelseId(), doedsfall.endringstype()));
		}

		if (doedsfall.personidenter().isEmpty()) {
			throw new ValideringFeiletFunctionalException(format("Doedsfall-hendelse med hendelseId=%s har ingen personidenter. Ignorerer hendelse." ,doedsfall.hendelseId()));
		}

		if (ENDRINGSTYPER_SOM_SKAL_HA_DOEDSDATO.contains(doedsfall.endringstype())) {
			if (doedsfall.doedsdato() == null) {
				throw new ValideringFeiletFunctionalException(format("Doedsfall-hendelse med hendelseId=%s og endringstype=%s mangler dødsdato. Ignorerer hendelse.", doedsfall.hendelseId(), doedsfall.endringstype()));
			}
		}
	}

}