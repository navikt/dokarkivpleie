package no.nav.dokarkivpleie.arkiverdokumentfiler;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.bucket.DokarkivmellomlagerConsumer;
import no.nav.dokarkivpleie.bucket.DokarkivmellomlagerTechnicalException;
import no.nav.dokarkivpleie.domain.DokumentFil;
import no.nav.dokarkivpleie.domain.DokumentFilOpplasting;
import no.nav.dokarkivpleie.repository.DokumentFilOpplastingRepository;
import no.nav.dokarkivpleie.repository.DokumentFilRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.CheckedInputStream;

import static no.nav.dokarkivpleie.arkiverdokumentfiler.Digest.getSha256Instance;
import static no.nav.dokarkivpleie.bucket.DokarkivmellomlagerConsumer.crc32c;
import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.ARKIVERES;

@Slf4j
@Component
public class DokumentFilArkiverer {

	public static final String KILDE = "teamdokumenthandtering:dokarkivpleie";

	private final DokumentFilOpplastingRepository dokumentFilOpplastingRepository;
	private final DokumentFilRepository dokumentFilRepository;
	private final DokarkivmellomlagerConsumer dokarkivmellomlagerConsumer;
	private final EntityManager entityManager;

	DokumentFilArkiverer(DokumentFilOpplastingRepository dokumentFilOpplastingRepository,
						 DokumentFilRepository dokumentFilRepository,
						 DokarkivmellomlagerConsumer dokarkivmellomlagerConsumer,
						 EntityManager entityManager) {
		this.dokumentFilOpplastingRepository = dokumentFilOpplastingRepository;
		this.dokumentFilRepository = dokumentFilRepository;
		this.dokarkivmellomlagerConsumer = dokarkivmellomlagerConsumer;
		this.entityManager = entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, timeoutString = "${dokarkivpleie.arkiverdokumentfiler.transaksjon-timeout}")
	public ArkiverDokumentFilResultat arkiver(UUID dokumentFilId) {
		return arkiverITransaksjon(dokumentFilId);
	}

	private ArkiverDokumentFilResultat arkiverITransaksjon(UUID dokumentFilId) {
		Optional<DokumentFilOpplasting> laastOpplasting = dokumentFilOpplastingRepository.findAndLockByDokumentFilIdAndTilstand(dokumentFilId, ARKIVERES);
		if (laastOpplasting.isEmpty()) {
			return ArkiverDokumentFilResultat.ikkeTatt();
		}

		DokumentFilOpplasting opplasting = laastOpplasting.get();
		if (dokumentFilRepository.existsByFilUuid(dokumentFilId)) {
			opplasting.arkivert(KILDE);
			log.info("dokumentFilId={} finnes allerede som dokumentFil. Setter tilstand={}", dokumentFilId, opplasting.getTilstand());
			return ArkiverDokumentFilResultat.alleredeArkivert();
		}

		if (opplasting.getAntallBytes() == null) {
			throw new DokumentFilIntegritetException("antallBytes mangler for dokumentFilId=%s".formatted(dokumentFilId));
		}

		MessageDigest sha256 = getSha256Instance();
		try (CheckedInputStream crc32cStream = dokarkivmellomlagerConsumer.hentDokument(dokumentFilId, opplasting.getCrc32cSjekksum());
			 DigestInputStream stream = new DigestInputStream(crc32cStream, sha256)) {

			DokumentFil dokumentFil = lagDokumentFil(opplasting, stream);
			dokumentFilRepository.persist(dokumentFil);
			entityManager.flush();

			validerSjekksum("CRC32C", dokumentFilId, opplasting.getCrc32cSjekksum(), crc32c(crc32cStream));

			opplasting.arkivert(KILDE);
			log.info("Arkiverte dokumentFilId={} som dokumentFil med id={}", dokumentFilId, dokumentFil.getDokumentFilId());
			return ArkiverDokumentFilResultat.arkivert();
		} catch (IOException e) {
			throw new DokarkivmellomlagerTechnicalException("Lesing av dokumentFilId=%s fra dokarkivmellomlager feilet".formatted(dokumentFilId), e);
		}
	}

	private static DokumentFil lagDokumentFil(DokumentFilOpplasting opplasting, DigestInputStream stream) {
		return DokumentFil.builder()
				.filUuid(opplasting.getDokumentFilId())
				.fil(Hibernate.getLobHelper().createBlob(stream, opplasting.getAntallBytes()))
				.opprettetKildeNavn(KILDE)
				.opprettetAv(KILDE)
				.build();
	}

	private static void validerSjekksum(String algoritme, UUID dokumentFilId, byte[] forventet, byte[] faktisk) {
		if (!Arrays.equals(forventet, faktisk)) {
			throw new DokumentFilIntegritetException("%s for dokumentFilId=%s matcher ikke. forventet=%s, faktisk=%s"
					.formatted(algoritme, dokumentFilId, hex(forventet), hex(faktisk)));
		}
	}

	private static String hex(byte[] bytes) {
		return bytes == null ? "null" : HexFormat.of().formatHex(bytes);
	}

}
