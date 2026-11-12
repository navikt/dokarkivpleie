package no.nav.dokarkivpleie;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import no.nav.dokarkivpleie.anticorruptionlayer.dokarkivmellomlager.InMemoryBucketStorage;
import no.nav.dokarkivpleie.arkiverdokumentfiler.ArkiverDokumentFilerScheduler;
import no.nav.dokarkivpleie.arkiverdokumentfiler.DokumentFilArkiverer;
import no.nav.dokarkivpleie.config.CoreConfig;
import no.nav.dokarkivpleie.config.DokarkivpleieProperties;
import no.nav.dokarkivpleie.config.RepositoryConfig;
import no.nav.dokarkivpleie.domain.DokumentFil;
import no.nav.dokarkivpleie.domain.DokumentFilOpplasting;
import no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand;
import no.nav.dokarkivpleie.repository.DokumentFilOpplastingRepository;
import no.nav.dokarkivpleie.repository.DokumentFilRepository;
import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.ARKIVERES;
import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.ARKIVERT;
import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.FEILET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest(classes = {
		ApplicationTestConfig.class,
		RepositoryConfig.class,
		CoreConfig.class
})
@EnableWireMock
@ActiveProfiles("itest")
class ArkiverDokumentFilerIT {

	public static final byte[] DOKUMENT_FIL_INNHOLD = "hei".getBytes();

	@Autowired
	private ArkiverDokumentFilerScheduler arkiverDokumentFilerScheduler;

	@Autowired
	private DokumentFilOpplastingRepository dokumentFilOpplastingRepository;

	@Autowired
	private InMemoryBucketStorage bucketStorage;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private DokumentFilRepository dokumentFilRepository;

	@Autowired
	private DokarkivpleieProperties dokarkivpleieProperties;

	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult(_ -> {
			entityManager.createQuery("delete from DokumentFil").executeUpdate();
			entityManager.createQuery("delete from DokumentFilOpplasting").executeUpdate();
		});
		bucketStorage.nullstill();
	}

	@Test
	void skalLagreDokumentFiler() throws SQLException, IOException {
		UUID dokumentFilId1 = UUID.randomUUID();
		lagOpplastingMedFil(dokumentFilId1, DOKUMENT_FIL_INNHOLD);
		lagOpplastingMedFil(UUID.randomUUID(), DOKUMENT_FIL_INNHOLD);
		lagOpplastingMedFil(UUID.randomUUID(), DOKUMENT_FIL_INNHOLD);
		lagOpplastingMedFil(UUID.randomUUID(), DOKUMENT_FIL_INNHOLD);
		lagOpplastingMedFil(UUID.randomUUID(), DOKUMENT_FIL_INNHOLD);

		kjoerJobb();

		DokumentFilOpplasting dokumentFilOpplasting = hentDokumentFilOpplasting(dokumentFilId1);
		assertThat(dokumentFilOpplasting.getTilstand()).isEqualTo(ARKIVERT);
		assertThat(dokumentFilOpplasting.getDatoArkivert()).isCloseTo(LocalDateTime.now(), within(Duration.ofMinutes(5)));
		DokumentFil dokumentFil1 = hentDokumentFil(dokumentFilId1);
		assertThat(IOUtils.toByteArray(dokumentFil1.getFil().getBinaryStream())).isEqualTo(DOKUMENT_FIL_INNHOLD);
	}


	@Test
	void skalArkivereAlleOpplastingerIArkiveres() {
		List<UUID> dokumentFilIder = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		dokumentFilIder.forEach(dokumentFilId -> lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD));

		kjoerJobb();

		assertThat(antallDokumentFiler()).isEqualTo(dokumentFilIder.size());
		dokumentFilIder.forEach(dokumentFilId -> {
			assertThat(hentDokumentFilOpplasting(dokumentFilId).getTilstand()).isEqualTo(ARKIVERT);
			DokumentFil dokumentFil = hentDokumentFil(dokumentFilId);
			assertThat(dokumentFil.getOpprettetKildeNavn()).isEqualTo(DokumentFilArkiverer.KILDE);
			assertThat(dokumentFil.getOpprettetAv()).isEqualTo(DokumentFilArkiverer.KILDE);
			assertThat(hentFilInnhold(dokumentFilId)).isEqualTo(DOKUMENT_FIL_INNHOLD);
		});
	}

	@Test
	void skalFjerneTemporaryHoldEtterArkivering() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD);
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isTrue();

		kjoerJobb();

		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isFalse();
	}

	@ParameterizedTest
	@EnumSource(value = DokumentFilOpplastingTilstand.class, names = "ARKIVERES", mode = EnumSource.Mode.EXCLUDE)
	void skalIkkeBehandleOpplastingerIAndreTilstander(DokumentFilOpplastingTilstand tilstand) {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplasting(opplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD).tilstand(tilstand).build());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(tilstand);
		assertThat(opplasting.getForsok()).isZero();
		assertThat(antallDokumentFiler()).isZero();
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isTrue();
	}

	@Test
	void skalIkkeFeileNaarIngenOpplastinger() {
		kjoerJobb();

		assertThat(antallDokumentFiler()).isZero();
	}

	@Test
	void skalBegrenseTilBatchStorrelse() {
		int batchStorrelse = dokarkivpleieProperties.getArkiverDokumentfiler().getBatchStorrelse();
		for (int i = 0; i < batchStorrelse + 1; i++) {
			lagOpplastingMedFil(UUID.randomUUID(), DOKUMENT_FIL_INNHOLD);
		}

		kjoerJobb();

		assertThat(antallDokumentFiler()).isEqualTo(batchStorrelse);
		assertThat(antallOpplastinger(ARKIVERT)).isEqualTo(batchStorrelse);
		assertThat(antallOpplastinger(ARKIVERES)).isEqualTo(1);
	}

	@Test
	void skalOkeForsokVedFeilVedLesing() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD);
		bucketStorage.feilVedLesing(dokumentFilId.toString());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(ARKIVERES);
		assertThat(opplasting.getForsok()).isEqualTo(1);
		assertThat(antallDokumentFiler()).isZero();
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isTrue();
	}

	@Test
	void skalArkivereVedNesteKjoringEtterTekniskFeil() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD);
		bucketStorage.feilVedLesing(dokumentFilId.toString());

		kjoerJobb();
		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(ARKIVERT);
		assertThat(opplasting.getForsok()).isEqualTo(1);
		assertThat(hentFilInnhold(dokumentFilId)).isEqualTo(DOKUMENT_FIL_INNHOLD);
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isFalse();
	}

	@Test
	void skalOkeForsokNaarObjektManglerIBucket() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplasting(opplasting(dokumentFilId, DOKUMENT_FIL_INNHOLD).build());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(ARKIVERES);
		assertThat(opplasting.getForsok()).isEqualTo(1);
		assertThat(antallDokumentFiler()).isZero();
	}

	@Test
	void skalSetteFeiletNaarMaksForsokErNaadd() {
		int maksForsok = dokarkivpleieProperties.getArkiverDokumentfiler().getMaksForsok();
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplasting(opplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD).forsok(maksForsok - 1).build());
		bucketStorage.feilVedLesing(dokumentFilId.toString());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(FEILET);
		assertThat(opplasting.getForsok()).isEqualTo(maksForsok);
		assertThat(antallDokumentFiler()).isZero();
	}

	@Test
	void skalSetteFeiletVedUlikCrc32c() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplasting(opplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD)
				.crc32cSjekksum(InMemoryBucketStorage.crc32c("annet innhold".getBytes()))
				.build());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(FEILET);
		assertThat(opplasting.getForsok()).isZero();
		assertThat(antallDokumentFiler()).isZero();
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isTrue();
	}

	@Test
	void skalSetteFeiletNaarAntallBytesMangler() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplasting(opplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD).antallBytes(null).build());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(FEILET);
		assertThat(opplasting.getForsok()).isZero();
		assertThat(antallDokumentFiler()).isZero();
	}

	@Test
	void skalIkkeRulleTilbakeAndreRaderVedFeil() {
		UUID feilendeDokumentFilId = UUID.randomUUID();
		lagOpplastingMedFil(feilendeDokumentFilId, DOKUMENT_FIL_INNHOLD);
		bucketStorage.feilVedLesing(feilendeDokumentFilId.toString());
		List<UUID> dokumentFilIder = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		dokumentFilIder.forEach(dokumentFilId -> lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD));

		kjoerJobb();

		dokumentFilIder.forEach(dokumentFilId -> assertThat(hentDokumentFilOpplasting(dokumentFilId).getTilstand()).isEqualTo(ARKIVERT));
		assertThat(antallDokumentFiler()).isEqualTo(dokumentFilIder.size());
		DokumentFilOpplasting feilende = hentDokumentFilOpplasting(feilendeDokumentFilId);
		assertThat(feilende.getTilstand()).isEqualTo(ARKIVERES);
		assertThat(feilende.getForsok()).isEqualTo(1);
	}

	@Test
	void skalArkivereSelvOmFjerningAvTemporaryHoldFeiler() {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD);
		bucketStorage.feilVedFjerningAvHold(dokumentFilId.toString());

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(ARKIVERT);
		assertThat(opplasting.getForsok()).isZero();
		assertThat(hentFilInnhold(dokumentFilId)).isEqualTo(DOKUMENT_FIL_INNHOLD);
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isTrue();
	}

	@Test
	void skalIkkeArkivereDobbeltNaarDokumentFilFinnes() {
		UUID dokumentFilId = UUID.randomUUID();
		byte[] eksisterendeInnhold = "eksisterende".getBytes();
		lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD);
		lagDokumentFil(dokumentFilId, eksisterendeInnhold);

		kjoerJobb();

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(ARKIVERT);
		assertThat(opplasting.getDatoArkivert()).isNotNull();
		assertThat(antallDokumentFiler()).isEqualTo(1);
		assertThat(hentFilInnhold(dokumentFilId)).isEqualTo(eksisterendeInnhold);
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isFalse();
	}

	@Test
	void skalHoppeOverRadSomErLaast() throws Exception {
		UUID dokumentFilId = UUID.randomUUID();
		lagOpplastingMedFil(dokumentFilId, DOKUMENT_FIL_INNHOLD);
		CountDownLatch laast = new CountDownLatch(1);
		CountDownLatch slipp = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<?> laasHolder = executor.submit(() -> transactionTemplate.executeWithoutResult(_ -> {
				entityManager.find(DokumentFilOpplasting.class, dokumentFilId, LockModeType.PESSIMISTIC_WRITE);
				laast.countDown();
				try {
					slipp.await(30, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}));
			assertThat(laast.await(10, TimeUnit.SECONDS)).isTrue();

			try {
				kjoerJobb();
			} finally {
				slipp.countDown();
			}
			laasHolder.get(10, TimeUnit.SECONDS);
		}

		DokumentFilOpplasting opplasting = hentDokumentFilOpplasting(dokumentFilId);
		assertThat(opplasting.getTilstand()).isEqualTo(ARKIVERES);
		assertThat(opplasting.getForsok()).isZero();
		assertThat(antallDokumentFiler()).isZero();
		assertThat(bucketStorage.harTemporaryHold(dokumentFilId.toString())).isTrue();
	}

	/// Legger innholdet i bucket og lagrer en opplasting i tilstand `ARKIVERES` som matcher innholdet.
	DokumentFilOpplasting lagOpplastingMedFil(UUID dokumentFilId, byte[] innhold) {
		return lagOpplasting(opplastingMedFil(dokumentFilId, innhold).build());
	}

	/// Legger innholdet i bucket og returnerer en builder for en opplasting som matcher innholdet.
	DokumentFilOpplasting.DokumentFilOpplastingBuilder opplastingMedFil(UUID dokumentFilId, byte[] innhold) {
		bucketStorage.leggTil(dokumentFilId.toString(), innhold);
		return opplasting(dokumentFilId, innhold);
	}

	/// Builder for en opplasting i tilstand `ARKIVERES` som matcher innholdet, uten å legge noe i bucket.
	static DokumentFilOpplasting.DokumentFilOpplastingBuilder opplasting(UUID dokumentFilId, byte[] innhold) {
		return DokumentFilOpplasting.builder()
				.dokumentFilId(dokumentFilId)
				.eksternDokumentReferanseId(UUID.randomUUID().toString())
				.mediaType("application/pdf")
				.tilstand(ARKIVERES)
				.sha256Sjekksum(sha256(innhold))
				.crc32cSjekksum(InMemoryBucketStorage.crc32c(innhold))
				.antallBytes((long) innhold.length)
				.opprettetKildeNavn("itest")
				.datoLastetOpp(LocalDateTime.now())
				.datoKlientLease(LocalDateTime.now());
	}

	/// Lagrer opplastingen i en egen transaksjon.
	DokumentFilOpplasting lagOpplasting(DokumentFilOpplasting opplasting) {
		transactionTemplate.executeWithoutResult(_ -> dokumentFilOpplastingRepository.persist(opplasting));
		return opplasting;
	}

	void lagDokumentFil(UUID filUuid, byte[] innhold) {
		transactionTemplate.executeWithoutResult(_ -> dokumentFilRepository.persist(DokumentFil.builder()
				.filUuid(filUuid)
				.fil(Hibernate.getLobHelper().createBlob(innhold))
				.opprettetKildeNavn("itest")
				.opprettetAv("itest")
				.build()));
	}

	void kjoerJobb() {
		arkiverDokumentFilerScheduler.kjoerPeriodiskJobb();
	}

	DokumentFilOpplasting hentDokumentFilOpplasting(UUID dokumentFilId) {
		return dokumentFilOpplastingRepository.findById(dokumentFilId).orElseThrow();
	}

	DokumentFil hentDokumentFil(UUID dokumentFilId) {
		return dokumentFilRepository.findByFilUuid(dokumentFilId).orElseThrow();
	}

	long antallDokumentFiler() {
		return entityManager.createQuery("select count(d) from DokumentFil d", Long.class).getSingleResult();
	}

	long antallOpplastinger(DokumentFilOpplastingTilstand tilstand) {
		return entityManager.createQuery("select count(o) from DokumentFilOpplasting o where o.tilstand = :tilstand", Long.class)
				.setParameter("tilstand", tilstand)
				.getSingleResult();
	}

	byte[] hentFilInnhold(UUID filUuid) {
		return transactionTemplate.execute(_ -> {
			Blob fil = hentDokumentFil(filUuid).getFil();
			try {
				return fil.getBytes(1, (int) fil.length());
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	static byte[] sha256(byte[] innhold) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(innhold);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
