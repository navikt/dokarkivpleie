package no.nav.dokarkivpleie.arkiverdokumentfiler;

public enum ArkiverDokumentFilResultat {

	/// Dokumentet er arkivert
	ARKIVERT,
	/// Dokumentet er allerede arkivert
	ALLEREDE_ARKIVERT,
	/// Raden er låst av en annen transaksjon eller har ikke lenger tilstand `ARKIVERING`.
	IKKE_TATT,
	/// Raden har feilet teknisk konfigurerbart antall ganger
	FEIL
}
