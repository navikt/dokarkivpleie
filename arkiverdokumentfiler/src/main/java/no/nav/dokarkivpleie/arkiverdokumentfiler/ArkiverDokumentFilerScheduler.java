package no.nav.dokarkivpleie.arkiverdokumentfiler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArkiverDokumentFilerScheduler {

	private final ArkiverDokumentFilerService arkiverDokumentFilerService;

	ArkiverDokumentFilerScheduler(ArkiverDokumentFilerService arkiverDokumentFilerService) {
		this.arkiverDokumentFilerService = arkiverDokumentFilerService;
	}

	@Scheduled(cron = "${dokarkivpleie.arkiverdokumentfiler.cronschedule}")
	public void kjoerPeriodiskJobb() {
		log.info("Starter ArkiverDokumentFiler");
		try {
			arkiverDokumentFilerService.arkiverDokumentFiler();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("ArkiverDokumentFiler ble avbrutt");
		} catch (Exception e) {
			log.error("ArkiverDokumentFiler feilet med exception: ", e);
		}
		log.info("ArkiverDokumentFiler er ferdig");
	}
}
