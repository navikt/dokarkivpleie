package no.nav.dokarkivpleie.arkiverdokumentfiler;

import no.nav.dokarkivpleie.DokarkivpleieFunctionalException;

/// Filen som er lastet ned matcher ikke forventet sjekksum eller størrelse. Skal ikke forsøkes på nytt.
public class DokumentFilIntegritetException extends DokarkivpleieFunctionalException {

	public DokumentFilIntegritetException(String message) {
		super(message);
	}
}
