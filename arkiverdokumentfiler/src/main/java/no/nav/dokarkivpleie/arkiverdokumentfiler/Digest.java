package no.nav.dokarkivpleie.arkiverdokumentfiler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Digest {

	private Digest() {
		// ingen instansiering
	}

	public static MessageDigest getSha256Instance() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
