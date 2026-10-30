package no.nav.dokarkivpleie.arkiverdokumentfiler;

import static no.nav.dokarkivpleie.arkiverdokumentfiler.ArkiverDokumentFilResultat.Utfall.ALLEREDE_ARKIVERT;
import static no.nav.dokarkivpleie.arkiverdokumentfiler.ArkiverDokumentFilResultat.Utfall.ARKIVERT;
import static no.nav.dokarkivpleie.arkiverdokumentfiler.ArkiverDokumentFilResultat.Utfall.IKKE_TATT;

public record ArkiverDokumentFilResultat(Utfall utfall) {

	public enum Utfall {
		ARKIVERT,
		ALLEREDE_ARKIVERT,
		/// Raden er låst av en annen transaksjon eller har ikke lenger tilstand `ARKIVERING`.
		IKKE_TATT,
		FEIL
	}

	public static ArkiverDokumentFilResultat arkivert() {
		return new ArkiverDokumentFilResultat(ARKIVERT);
	}

	public static ArkiverDokumentFilResultat alleredeArkivert() {
		return new ArkiverDokumentFilResultat(ALLEREDE_ARKIVERT);
	}

	public static ArkiverDokumentFilResultat ikkeTatt() {
		return new ArkiverDokumentFilResultat(IKKE_TATT);
	}

	public static ArkiverDokumentFilResultat feil() {
		return new ArkiverDokumentFilResultat(IKKE_TATT);
	}
}
