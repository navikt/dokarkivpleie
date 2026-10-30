package no.nav.dokarkivpleie.arkiverdokumentfiler;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.domain.DokumentFilOpplasting;
import no.nav.dokarkivpleie.repository.DokumentFilOpplastingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

import static no.nav.dokarkivpleie.arkiverdokumentfiler.DokumentFilArkiverer.KILDE;
import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.ARKIVERES;

/// Registrerer feil på en [DokumentFilOpplasting] i en egen transaksjon etter at arkiveringen er rullet tilbake.
@Slf4j
@Component
public class DokumentFilOpplastingFeilhandterer {

	private final DokumentFilOpplastingRepository dokumentFilOpplastingRepository;

	DokumentFilOpplastingFeilhandterer(DokumentFilOpplastingRepository dokumentFilOpplastingRepository) {
		this.dokumentFilOpplastingRepository = dokumentFilOpplastingRepository;
	}

	@Transactional
	public void markerFeilet(UUID dokumentFilId) {
		oppdater(dokumentFilId, opplasting -> {
			opplasting.feilet(KILDE);
			log.error("dokumentFilId={} er satt til tilstand={}", dokumentFilId, opplasting.getTilstand());
		});
	}

	@Transactional
	public void registrerFeiletForsok(UUID dokumentFilId, int maksForsok) {
		oppdater(dokumentFilId, opplasting -> {
			opplasting.registrerFeiletForsok(maksForsok, KILDE);
			log.warn("dokumentFilId={} har forsok={} av maksForsok={}, tilstand={}",
					dokumentFilId, opplasting.getForsok(), maksForsok, opplasting.getTilstand());
		});
	}

	private void oppdater(UUID dokumentFilId, Consumer<DokumentFilOpplasting> endring) {
		dokumentFilOpplastingRepository.findAndLockByDokumentFilIdAndTilstand(dokumentFilId, ARKIVERES)
				.ifPresentOrElse(endring,
						() -> log.warn("Kunne ikke registrere feil for dokumentFilId={}. Raden er låst eller har ikke tilstand={}", dokumentFilId, ARKIVERES));
	}
}
