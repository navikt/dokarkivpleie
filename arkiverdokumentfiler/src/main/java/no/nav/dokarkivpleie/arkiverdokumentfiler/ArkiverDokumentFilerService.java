package no.nav.dokarkivpleie.arkiverdokumentfiler;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.config.DokarkivpleieProperties;
import no.nav.dokarkivpleie.repository.DokumentFilOpplastingRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.ARKIVERES;

@Slf4j
@Service
public class ArkiverDokumentFilerService {

	private final DokumentFilOpplastingRepository dokumentFilOpplastingRepository;
	private final ArkiverDokumentFilOppgave arkiverDokumentFilOppgave;
	private final DokarkivpleieProperties.Arkiverdokumentfiler properties;

	ArkiverDokumentFilerService(DokumentFilOpplastingRepository dokumentFilOpplastingRepository,
								ArkiverDokumentFilOppgave arkiverDokumentFilOppgave,
								DokarkivpleieProperties properties) {
		this.dokumentFilOpplastingRepository = dokumentFilOpplastingRepository;
		this.arkiverDokumentFilOppgave = arkiverDokumentFilOppgave;
		this.properties = properties.getArkiverDokumentfiler();
	}

	public void arkiverDokumentFiler() throws InterruptedException {
		List<UUID> dokumentFilIder = dokumentFilOpplastingRepository.findDokumentFilIdByTilstand(ARKIVERES, Limit.of(properties.getBatchStorrelse()));
		log.info("ArkiverDokumentfiler fant {} dokumentfiler med tilstand={}", dokumentFilIder.size(), ARKIVERES);
		if (dokumentFilIder.isEmpty()) {
			return;
		}

		Semaphore samtidige = new Semaphore(properties.getMaksSamtidige());
		try (StructuredTaskScope<ArkiverDokumentFilResultat, Void> scope = StructuredTaskScope.open(Joiner.awaitAll(),
				configuration -> configuration.withTimeout(Duration.ofMinutes(5)))) {
			List<Subtask<ArkiverDokumentFilResultat>> subtasks = dokumentFilIder.stream()
					.map(dokumentFilId -> scope.fork(() -> {
						samtidige.acquire();
						try {
							return arkiverDokumentFilOppgave.utfor(dokumentFilId);
						} finally {
							samtidige.release();
						}
					}))
					.toList();

			scope.join();

			for (int i = 0; i < subtasks.size(); i++) {
				Subtask<ArkiverDokumentFilResultat> subtask = subtasks.get(i);
				if (subtask.state() == Subtask.State.FAILED) {
					log.error("Uventet feil ved arkivering av dokumentFilId={}", dokumentFilIder.get(i), subtask.exception());
				}
			}
		}
	}
}
