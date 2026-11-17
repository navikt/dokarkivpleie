package no.nav.dokarkivpleie.arkiverdokumentfiler;

import org.jspecify.annotations.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.CRC32C;
import java.util.zip.CheckedInputStream;

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

	public static byte[] crc32c(byte[] innhold) {
		CRC32C crc32c = new CRC32C();
		crc32c.update(innhold);
		int verdi = (int) crc32c.getValue();
		return createBigEndian(verdi);
	}

	public static byte[] crc32c(CheckedInputStream stream) {
		int verdi = (int) stream.getChecksum().getValue();
		return createBigEndian(verdi);
	}

	private static byte @NonNull [] createBigEndian(int verdi) {
		return new byte[]{(byte) (verdi >>> 24), (byte) (verdi >>> 16), (byte) (verdi >>> 8), (byte) verdi};
	}

	public static String hex(byte[] bytes) {
		return bytes == null ? "null" : HexFormat.of().formatHex(bytes);
	}
}
