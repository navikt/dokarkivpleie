package no.nav.dokarkivpleie.anticorruptionlayer.dokarkivmellomlager;

import no.nav.dokarkivpleie.bucket.BucketObjekt;
import no.nav.dokarkivpleie.bucket.BucketStorage;
import no.nav.dokarkivpleie.bucket.DokarkivmellomlagerTechnicalException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32C;

/// [BucketStorage] i minnet for tester.
@Component
@Profile("itest")
public class InMemoryBucketStorage implements BucketStorage {

	private final Map<String, Objekt> objekter = new ConcurrentHashMap<>();
	private final Set<String> feilVedLesing = ConcurrentHashMap.newKeySet();
	private final Set<String> feilVedFjerningAvHold = ConcurrentHashMap.newKeySet();

	public void leggTil(String objektnavn, byte[] innhold) {
		objekter.put(objektnavn, new Objekt(innhold.clone(), true));
	}

	public boolean harTemporaryHold(String objektnavn) {
		return objekter.get(objektnavn).temporaryHold();
	}

	/// Neste kall til [#lesObjekt] for objektet kaster [DokarkivmellomlagerTechnicalException].
	public void feilVedLesing(String objektnavn) {
		feilVedLesing.add(objektnavn);
	}

	/// Kall til [#fjernTemporaryHold] for objektet kaster [DokarkivmellomlagerTechnicalException].
	public void feilVedFjerningAvHold(String objektnavn) {
		feilVedFjerningAvHold.add(objektnavn);
	}

	public void nullstill() {
		objekter.clear();
		feilVedLesing.clear();
		feilVedFjerningAvHold.clear();
	}

	public static byte[] crc32c(byte[] innhold) {
		CRC32C crc32c = new CRC32C();
		crc32c.update(innhold);
		int verdi = (int) crc32c.getValue();
		return new byte[]{(byte) (verdi >>> 24), (byte) (verdi >>> 16), (byte) (verdi >>> 8), (byte) verdi};
	}

	@Override
	public Optional<BucketObjekt> hentMetadata(String objektnavn) {
		return Optional.ofNullable(objekter.get(objektnavn))
				.map(objekt -> new BucketObjekt(objektnavn, crc32c(objekt.innhold()), objekt.innhold().length, objekt.temporaryHold()));
	}

	@Override
	public InputStream lesObjekt(String objektnavn) {
		if (feilVedLesing.remove(objektnavn)) {
			throw new DokarkivmellomlagerTechnicalException("Simulert feil ved lesing av objekt=%s".formatted(objektnavn), null);
		}
		return new ByteArrayInputStream(hentObjekt(objektnavn).innhold());
	}

	@Override
	public void fjernTemporaryHold(String objektnavn) {
		if (feilVedFjerningAvHold.contains(objektnavn)) {
			throw new DokarkivmellomlagerTechnicalException("Simulert feil ved fjerning av temporary hold for objekt=%s".formatted(objektnavn), null);
		}
		objekter.computeIfPresent(objektnavn, (_, objekt) -> new Objekt(objekt.innhold(), false));
	}

	private Objekt hentObjekt(String objektnavn) {
		Objekt objekt = objekter.get(objektnavn);
		if (objekt == null) {
			throw new DokarkivmellomlagerTechnicalException("Objekt=%s finnes ikke".formatted(objektnavn), null);
		}
		return objekt;
	}

	private record Objekt(byte[] innhold, boolean temporaryHold) {
	}
}
