package no.nav.dokarkivpleie.bucket;

import no.nav.dokarkivpleie.arkiverdokumentfiler.DokumentFilIntegritetException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.CRC32C;
import java.util.zip.CheckedInputStream;

@Component
public class DokarkivmellomlagerConsumer {

	private final BucketStorage bucketStorage;

	DokarkivmellomlagerConsumer(BucketStorage bucketStorage) {
		this.bucketStorage = bucketStorage;
	}

	public CheckedInputStream hentDokument(UUID dokumentFilId, byte[] forventetCrc32c) {
		BucketObjekt blobInfo = bucketStorage.hentMetadata(dokumentFilId.toString())
				.orElseThrow(() -> new DokarkivmellomlagerTechnicalException("Fant ikke dokumentFilId=%s i dokarkivmellomlager".formatted(dokumentFilId), null));

		if (!Arrays.equals(blobInfo.crc32c(), forventetCrc32c)) {
			throw new DokumentFilIntegritetException("CRC32C for dokumentFilId=%s i dokarkivmellomlager=%s matcher ikke forventet=%s"
					.formatted(dokumentFilId, hex(blobInfo.crc32c()), hex(forventetCrc32c)));
		}

		return new CheckedInputStream(bucketStorage.lesObjekt(dokumentFilId.toString()), new CRC32C());
	}

	public void fjernTemporaryHold(String dokumentFilId) {
		bucketStorage.fjernTemporaryHold(dokumentFilId);
	}

	public static byte[] crc32c(CheckedInputStream stream) {
		int verdi = (int) stream.getChecksum().getValue();
		return new byte[]{(byte) (verdi >>> 24), (byte) (verdi >>> 16), (byte) (verdi >>> 8), (byte) verdi};
	}

	private static String hex(byte[] bytes) {
		return bytes == null ? "null" : HexFormat.of().formatHex(bytes);
	}
}
