package no.nav.dokarkivpleie.bucket;

import java.io.InputStream;
import java.util.Optional;

public interface BucketStorage {

	Optional<BucketObjekt> hentMetadata(String objektnavn);

	InputStream lesObjekt(String objektnavn);

	void fjernTemporaryHold(String objektnavn);
}
