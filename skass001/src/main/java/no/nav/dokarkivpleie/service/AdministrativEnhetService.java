package no.nav.dokarkivpleie.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.Arkivsak;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusConsumer;
import no.nav.dokarkivpleie.consumers.dvh.DatavarehusResponse.AdministrativEnhet;
import no.nav.dokarkivpleie.domain.Journalpost;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AdministrativEnhetService {

	private Map<String, List<AdministrativEnhet>> administrativEnhetMap;
	private final DatavarehusConsumer datavarehusConsumer;

	public static final String APPLIKASJON_INFOTRYGD = "IT01";
	public static final String APPLIKASJON_ARENA = "AO01";
	public static final String KONTORTYPE_NORG = "NORGENHET";
	public static final String KONTORTYPE_INFOTRYGD = "INFOENHET";
	public static final String KONTORTYPE_ARENA = "ARENAENHET";

	public AdministrativEnhetService(DatavarehusConsumer datavarehusConsumer) {
		this.datavarehusConsumer = datavarehusConsumer;
	}

	public void hentAdministrativeEnheterFraDatavarehus() {
		log.info("Populerer administrativEnhetMap med data fra datavarehus");

		List<AdministrativEnhet> administrativEnheter = datavarehusConsumer.hentAlleAdministrativeEnheter().getItems();
		administrativEnhetMap = administrativEnheter.stream()
				.collect(Collectors.groupingBy(AdministrativEnhet::journalfoerendeEnhet));

		log.info("Fant {} administrative enheter fra datavarehus", administrativEnhetMap.size());
	}

	public String hentHistoriskNavnForAdministrativEnhet(Journalpost eldsteFerdigstilteJournalpost, Arkivsak arkivsak) {
		Optional<String> administrativEnhetOptional = finnHistoriskKontornavnForAdministrativEnhet(
				eldsteFerdigstilteJournalpost.journalfoerendeEnhet(), eldsteFerdigstilteJournalpost.journaldato(), arkivsak.applikasjon());

		if (administrativEnhetOptional.isEmpty()) {
			return null;
		}
		return administrativEnhetOptional.get();
	}

	private Optional<String> finnHistoriskKontornavnForAdministrativEnhet(String journalfoerendeEnhet, LocalDate journalfoertDato, String applikasjon) {
		List<AdministrativEnhet> kontorer = administrativEnhetMap.get(journalfoerendeEnhet);
		if (kontorer == null) {
			return Optional.empty();
		}

		List<AdministrativEnhet> gyldigeKontorer = kontorer.stream()
				.filter(ae -> varAdministrativEnhetGyldigNaarJournalpostBleJournalfoert(journalfoertDato, ae))
				.toList();

		if (gyldigeKontorer.isEmpty()) {
			return Optional.empty();
		}

		if (gyldigeKontorer.size() == 1) {
			return Optional.of(gyldigeKontorer.getFirst().kontornavn());
		}

		if (APPLIKASJON_INFOTRYGD.equals(applikasjon) && harDataForKontor(gyldigeKontorer, KONTORTYPE_INFOTRYGD)) {
			return hentKontornavn(gyldigeKontorer, KONTORTYPE_INFOTRYGD);
		}
		if (APPLIKASJON_ARENA.equals(applikasjon) && harDataForKontor(gyldigeKontorer, KONTORTYPE_ARENA)) {
			return hentKontornavn(gyldigeKontorer, KONTORTYPE_ARENA);
		}

		if (harDataForKontor(gyldigeKontorer, KONTORTYPE_NORG)) {
			return hentKontornavn(gyldigeKontorer, KONTORTYPE_NORG);
		}

		if (harDataForKontor(gyldigeKontorer, KONTORTYPE_INFOTRYGD)) {
			return hentKontornavn(gyldigeKontorer, KONTORTYPE_INFOTRYGD);
		}

		if (harDataForKontor(gyldigeKontorer, KONTORTYPE_ARENA)) {
			return hentKontornavn(gyldigeKontorer, KONTORTYPE_ARENA);
		}

		return Optional.empty();
	}

	private static boolean varAdministrativEnhetGyldigNaarJournalpostBleJournalfoert(LocalDate journalfoertDato, AdministrativEnhet administrativEnhet) {
		return erDatoLikEllerEtter(journalfoertDato, administrativEnhet.gyldigFraDato()) && erDatoLikEllerFoer(journalfoertDato, administrativEnhet.gyldigTilDato());
	}

	private static boolean erDatoLikEllerFoer(LocalDate journalfoertDato, LocalDate gyldigTilDato) {
		return journalfoertDato.isEqual(gyldigTilDato) || journalfoertDato.isBefore(gyldigTilDato);
	}

	private static boolean erDatoLikEllerEtter(LocalDate journalfoertDato, LocalDate gyldigFraDato) {
		return journalfoertDato.isEqual(gyldigFraDato) || journalfoertDato.isAfter(gyldigFraDato);
	}

	private boolean harDataForKontor(List<AdministrativEnhet> gyldigeKontorer, String fagsystem) {
		return gyldigeKontorer.stream()
				.anyMatch(ae -> ae.kontortype().equals(fagsystem));
	}

	private Optional<String> hentKontornavn(List<AdministrativEnhet> gyldigeKontorer, String fagsystem) {
		return gyldigeKontorer.stream()
				.filter(ae -> fagsystem.equals(ae.kontortype()))
				.map(AdministrativEnhet::kontornavn)
				.findFirst();
	}

}