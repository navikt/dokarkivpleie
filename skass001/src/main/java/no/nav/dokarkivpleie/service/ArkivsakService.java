package no.nav.dokarkivpleie.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.Arkivsak;
import no.nav.dokarkivpleie.domain.Avleveringsstatus;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.domain.Journalpost;
import no.nav.dokarkivpleie.domain.Kassasjonsstatus;
import no.nav.dokarkivpleie.domain.Sak;
import no.nav.dokarkivpleie.domain.Saksstatus;
import no.nav.dokarkivpleie.repository.AdministrativEnhetJdbcRepository;
import no.nav.dokarkivpleie.repository.JournalpostJdbcRepository;
import no.nav.dokarkivpleie.repository.SakRepository;
import no.nav.dokarkivpleie.repository.SlettebestillingRepository;
import no.nav.dokarkivpleie.slack.SlackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.dokarkivpleie.MerkSakerBevaringstidPassertService.DOKARKIVPLEIE;
import static no.nav.dokarkivpleie.MerkSakerBevaringstidPassertService.MERK_SAKER_BEVARINGSTID_PASSERT;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.BEVARINGSTID_PASSERT;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.BEVARINGSTID_PASSERT_DOK_KASSASJON_BESTILT;
import static no.nav.dokarkivpleie.domain.Kassasjonsstatus.KLAR_FOR_KASSASJON;
import static no.nav.dokarkivpleie.domain.Slettebestilling.lagSlettebestilling;
import static no.nav.dokarkivpleie.service.ArkivsakMapper.mapSakerTilArkivsaker;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Service
public class ArkivsakService {

	private final SakRepository sakRepository;
	private final JournalpostJdbcRepository journalpostJdbcRepository;
	private final AdministrativEnhetService administrativEnhetService;
	private final AdministrativEnhetJdbcRepository administrativEnhetJdbcRepository;
	private final SlettebestillingRepository slettebestillingRepository;
	private final SlackService slackService;

	ArkivsakService(SakRepository sakRepository,
					JournalpostJdbcRepository journalpostJdbcRepository,
					AdministrativEnhetService administrativEnhetService,
					AdministrativEnhetJdbcRepository administrativEnhetJdbcRepository,
					SlettebestillingRepository slettebestillingRepository,
					SlackService slackService) {
		this.sakRepository = sakRepository;
		this.journalpostJdbcRepository = journalpostJdbcRepository;
		this.administrativEnhetService = administrativEnhetService;
		this.administrativEnhetJdbcRepository = administrativEnhetJdbcRepository;
		this.slettebestillingRepository = slettebestillingRepository;
		this.slackService = slackService;
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void kasserSakerForDoedePersoner(List<String> doedePersonerPartisjon, Fagomraade fagomraade) {
		Set<Sak> sakerForDoedePersoner = sakRepository.finnUkasserteSakerForBrukere(doedePersonerPartisjon, fagomraade.getKode());

		List<Arkivsak> arkivsaker = mapSakerTilArkivsaker(sakerForDoedePersoner);

		for (Arkivsak arkivsak : arkivsaker) {
			List<Long> saksIderTilArkivsak = arkivsak.saksIder();

			if (arkivsak.harBaadeAapneOgLukkedeSaker()) {
				log.error("Arkivsak inneholder saker med både åpne og lukkede saksstatuser med saksIder={}. Avbryter behandling av arkivsak.", saksIderTilArkivsak);
				slackService.sendMelding("Ugyldig arkivsak!", "Arkivsak inneholder saker med både åpne og lukkede saksstatuser med saksIder=%s.".formatted(saksIderTilArkivsak));
				continue;
			}

			if (arkivsak.harKunAapneSaker()) {
				populerArkivsakMedJournalposter(arkivsak);

				if (arkivsak.harJournalposterIMidlertidigeStatuser()) {
					log.info("Kan ikke avslutte arkivsak med saksIder={} siden journalpoststatuser={} inneholder midlertidige statuser. Avbryter behandling av arkivsak.", saksIderTilArkivsak, arkivsak.journalpoststatuser());
					continue;
				}

				if (arkivsak.harIngenFerdigstilteJournalposter()) {
					log.info("Arkivsak har ingen ferdigstilte journalposter. Avbryter saker={} knyttet til tom arkivsak.", saksIderTilArkivsak);
					avbrytArkivsak(arkivsak);
					continue;
				} else {
					String administrativEnhetNavn = finnAdministrativEnhet(arkivsak, fagomraade);
					if (administrativEnhetNavn == null) {
						log.warn("Fant ikke administrativ enhet for arkivsak med saksIder={}. Avbryter behandling av arkivsak.", saksIderTilArkivsak);
						continue;
					}
					avsluttArkivsak(arkivsak, administrativEnhetNavn);
				}
			}

			if (fagomraade.getAvleverMedDok()) {
				oppdaterKassasjonsstatus(arkivsak, BEVARINGSTID_PASSERT);
			} else {
				lagreSlettebestillingerForArkivsak(saksIderTilArkivsak, fagomraade.getBevaringstid());
				oppdaterKassasjonsstatus(arkivsak, BEVARINGSTID_PASSERT_DOK_KASSASJON_BESTILT);
			}

			log.info("Behandling av arkivsaker for opptil 200 personer er ferdig");
		}
	}

	private void populerArkivsakMedJournalposter(Arkivsak arkivsak) {
		var journalposter = journalpostJdbcRepository.hentJournalposterForSaker(arkivsak.saksIder());
		arkivsak.journalposter().addAll(journalposter);
	}

	private String finnAdministrativEnhet(Arkivsak arkivsak, Fagomraade fagomraade) {
		String enhetsnavnFraDvh = hentNavnFraDvh(arkivsak);
		if (isBlank(enhetsnavnFraDvh)) {
			log.info("Fant ikke historisk navn for administrativ enhet i datavarehus for arkivsak med saksIder={}. Prøver å hente fra administrativenhet-tabellen i joark.", arkivsak.saksIder());
			return administrativEnhetJdbcRepository.hentNavnForAdministrativEnhet(fagomraade.getKode(), arkivsak.finnOpprettetTidspunktForEldsteSak());
		}
		return enhetsnavnFraDvh;
	}

	public String hentNavnFraDvh(Arkivsak arkivsak) {
		Optional<Journalpost> eldsteFerdigstilteJournalpostOptional = arkivsak.finnEldsteFerdigstilteJournalpostMedJournalfoerendeEnhetUlikMaskinell();
		if (eldsteFerdigstilteJournalpostOptional.isPresent()) {
			String navnFraDvh = administrativEnhetService.hentHistoriskNavnForAdministrativEnhet(eldsteFerdigstilteJournalpostOptional.get(), arkivsak);
			if (!isBlank(navnFraDvh)) {
				return navnFraDvh;
			}
		}
		return null;
	}

	private void avbrytArkivsak(Arkivsak arkivsak) {
		arkivsak.saker().forEach(sak -> {
			sak.setSaksstatus(Saksstatus.AVBRUTT);
			sak.setAvleveringsstatus(Avleveringsstatus.AVBRUTT);
			sak.setKassasjonsstatus(KLAR_FOR_KASSASJON);
			sak.setEndretAv(MERK_SAKER_BEVARINGSTID_PASSERT);
			sak.setEndretKildeNavn(DOKARKIVPLEIE);
			sak.setDatoEndret(LocalDateTime.now());
		});
	}

	private void avsluttArkivsak(Arkivsak arkivsak, String administrativEnhet) {
		arkivsak.saker().forEach(sak -> {
			sak.setSaksstatus(Saksstatus.AVSLUTTET);
			sak.setEndretAv(MERK_SAKER_BEVARINGSTID_PASSERT);
			sak.setEndretKildeNavn(DOKARKIVPLEIE);
			sak.setDatoEndret(LocalDateTime.now());
			sak.setDatoAvsluttet(LocalDateTime.now());
			sak.setAvsluttetAv(MERK_SAKER_BEVARINGSTID_PASSERT);
			sak.setAvsluttetKildeNavn(DOKARKIVPLEIE);
			sak.setDatoSakOpprettet(arkivsak.finnEldsteFerdigstilteJournalpostMedJournalfoerendeEnhetUlikMaskinell().get().opprettetdato());
			sak.setAdministrativEnhet(administrativEnhet);
			sak.setSaksansvarlig(administrativEnhet);
		});
	}

	private void oppdaterKassasjonsstatus(Arkivsak arkivsak, Kassasjonsstatus kassasjonsstatus) {
		arkivsak.saker().forEach(sak -> {
			sak.setKassasjonsstatus(kassasjonsstatus);
			sak.setEndretAv(MERK_SAKER_BEVARINGSTID_PASSERT);
			sak.setEndretKildeNavn(DOKARKIVPLEIE);
			sak.setDatoEndret(LocalDateTime.now());
		});
	}

	private void lagreSlettebestillingerForArkivsak(List<Long> saksIder, String begrunnelse) {
		saksIder.forEach(saksId ->
				slettebestillingRepository.persist(lagSlettebestilling(saksId, begrunnelse))
		);
	}
}