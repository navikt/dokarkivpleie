package no.nav.dokarkivpleie.bucket;

import no.nav.dokarkivpleie.DokarkivpleieTechnicalException;

public class DokarkivmellomlagerTechnicalException extends DokarkivpleieTechnicalException {

	public DokarkivmellomlagerTechnicalException(String message, Throwable cause) {
		super(message);
		if (cause != null) {
			initCause(cause);
		}
	}
}
