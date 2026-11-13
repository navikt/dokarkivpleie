package no.nav.dokarkivpleie.bucket;

/// @param objektnavn     navn på objektet i bucket
/// @param crc32c         CRC32C sjekksum, 4 bytes big-endian
public record BucketObjekt(String objektnavn, byte[] crc32c) {
}
