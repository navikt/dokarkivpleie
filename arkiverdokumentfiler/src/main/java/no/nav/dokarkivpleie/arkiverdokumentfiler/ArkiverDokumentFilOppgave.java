package no.nav.dokarkivpleie.arkiverdokumentfiler;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.bucket.DokarkivmellomlagerConsumer;
import no.nav.dokarkivpleie.config.DokarkivpleieProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static no.nav.dokarkivpleie.arkiverdokumentfiler.ArkiverDokumentFilResultat.Utfall.ALLEREDE_ARKIVERT;
import static no.nav.dokarkivpleie.arkiverdokumentfiler.ArkiverDokumentFilResultat.Utfall.ARKIVERT;

/// Arkiverer én DokumentFil
@Slf4j
@Component
public class ArkiverDokumentFilOppgave {

	private final DokumentFilArkiverer dokumentFilArkiverer;
	private final DokumentFilOpplastingFeilhandterer feilhandterer;
	private final DokarkivmellomlagerConsumer dokarkivmellomlagerConsumer;
	private final DokarkivpleieProperties.Arkiverdokumentfiler properties;

	ArkiverDokumentFilOppgave(DokumentFilArkiverer dokumentFilArkiverer,
							  DokumentFilOpplastingFeilhandterer feilhandterer,
							  DokarkivmellomlagerConsumer dokarkivmellomlagerConsumer,
							  DokarkivpleieProperties properties) {
		this.dokumentFilArkiverer = dokumentFilArkiverer;
		this.feilhandterer = feilhandterer;
		this.dokarkivmellomlagerConsumer = dokarkivmellomlagerConsumer;
		this.properties = properties.getArkiverDokumentfiler();
	}

	public ArkiverDokumentFilResultat utfor(UUID dokumentFilId) {
		try {
			ArkiverDokumentFilResultat resultat = dokumentFilArkiverer.arkiver(dokumentFilId);
			if (resultat.utfall() == ARKIVERT || resultat.utfall() == ALLEREDE_ARKIVERT) {
				fjernTemporaryHold(dokumentFilId);
			} else {
				log.info("Hopper over dokumentFilId={}. Raden er låst eller ikke lenger i tilstand ARKIVERING", dokumentFilId);
			}
			return resultat;
		} catch (DokumentFilIntegritetException e) {
			log.error("Integritetssjekk feilet for dokumentFilId={}: {}", dokumentFilId, e.getMessage());
			registrerFeil(dokumentFilId, () -> feilhandterer.markerFeilet(dokumentFilId));
			return ArkiverDokumentFilResultat.feil();
		} catch (Exception e) {
			log.warn("Arkivering av dokumentFilId={} feilet", dokumentFilId, e);
			registrerFeil(dokumentFilId, () -> feilhandterer.registrerFeiletForsok(dokumentFilId, properties.getMaksForsok()));
			return ArkiverDokumentFilResultat.feil();
		}

	}

	private void fjernTemporaryHold(UUID dokumentFilId) {
		try {
			dokarkivmellomlagerConsumer.fjernTemporaryHold(dokumentFilId.toString());
		} catch (Exception e) {
			log.error("Klarte ikke fjerne temporary hold for arkivert dokumentFilId={}", dokumentFilId, e);
		}
	}

	private static void registrerFeil(UUID dokumentFilId, Runnable registrering) {
		try {
			registrering.run();
		} catch (Exception e) {
			log.error("Registrering av feil for dokumentFilId={} feilet", dokumentFilId, e);
		}
	}
}
