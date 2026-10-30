package no.nav.dokarkivpleie.bucket;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Base64;
import java.util.Optional;

/// [BucketStorage] mot Google Cloud Storage.
public class DokarkivmellomlagerBucketStorage implements BucketStorage {

	private final Storage storage;
	private final String bucket;

	public DokarkivmellomlagerBucketStorage(Storage storage, String bucket) {
		this.storage = storage;
		this.bucket = bucket;
	}

	@Override
	public Optional<BucketObjekt> hentMetadata(String objektnavn) {
		try {
			return Optional.ofNullable(storage.get(blobId(objektnavn)))
					.map(DokarkivmellomlagerBucketStorage::tilBucketObjekt);
		} catch (StorageException e) {
			throw new DokarkivmellomlagerTechnicalException("Henting av metadata for objekt=%s feilet".formatted(objektnavn), e);
		}
	}

	@Override
	public InputStream lesObjekt(String objektnavn) {
		try {
			return Channels.newInputStream(storage.reader(blobId(objektnavn)));
		} catch (StorageException e) {
			throw new DokarkivmellomlagerTechnicalException("Lesing av objekt=%s feilet".formatted(objektnavn), e);
		}
	}

	@Override
	public void fjernTemporaryHold(String objektnavn) {
		try {
			Blob blob = storage.get(blobId(objektnavn));
			if (blob == null) {
				throw new DokarkivmellomlagerTechnicalException("Objekt=%s finnes ikke".formatted(objektnavn), null);
			}
			storage.update(blob.toBuilder().setTemporaryHold(false).build());
		} catch (StorageException e) {
			throw new DokarkivmellomlagerTechnicalException("Fjerning av temporary hold for objekt=%s feilet".formatted(objektnavn), e);
		}
	}

	private BlobId blobId(String objektnavn) {
		return BlobId.of(bucket, objektnavn);
	}

	private static BucketObjekt tilBucketObjekt(Blob blob) {
		byte[] crc32c = blob.getCrc32c() == null ? null : Base64.getDecoder().decode(blob.getCrc32c());
		long antallBytes = blob.getSize() == null ? 0 : blob.getSize();
		return new BucketObjekt(blob.getName(), crc32c, antallBytes, Boolean.TRUE.equals(blob.getTemporaryHold()));
	}
}
