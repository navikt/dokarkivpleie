package no.nav.dokarkivpleie.bucket;

import java.io.InputStream;
import java.util.Optional;

public interface BucketStorage {

	/// @return metadata for objektet, eller tom dersom objektet ikke finnes
	Optional<BucketObjekt> hentMetadata(String objektnavn);

	/// Åpner en strøm mot innholdet i objektet. Kaller er ansvarlig for å lukke strømmen.
	InputStream lesObjekt(String objektnavn);

	void fjernTemporaryHold(String objektnavn);
}
