package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.consumers.pdl.PdlConsumer;
import no.nav.dokarkivpleie.consumers.pdl.PdlHentIdenterResponse.PdlIdenter;
import no.nav.dokarkivpleie.repository.SakRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.dokarkivpleie.DoedsfallhendelseValidator.validerDoedsfall;

@Slf4j
@Service
public class Karp001Service {

	public static final String OPPLYSNINGSTYPE_DOEDSFALL = "DOEDSFALL_V1";

	private final SakRepository sakRepository;
	private final PdlConsumer pdlConsumer;

	public Karp001Service(SakRepository sakRepository, PdlConsumer pdlConsumer) {
		this.sakRepository = sakRepository;
		this.pdlConsumer = pdlConsumer;
	}

	public void behandleDoedsfallhendelseFraLeesah(Doedsfallhendelse doedsfall) {		;
		log.info("Doedsfall-hendelse med endringstype={} og hendelseId={} mottatt.", doedsfall.endringstype(), doedsfall.hendelseId());
		validerDoedsfall(doedsfall);

		switch (doedsfall.endringstype()) {
			case OPPRETTET, KORRIGERT -> {
				PdlIdenter identer = pdlConsumer.hentAlleIdenterForIdent(doedsfall.foerstePersonident());

				String nyesteFnrEllerNpid = identer.nyesteFnrEllerNpid();
				if (nyesteFnrEllerNpid == null) {
					log.info("Doedsfall-hendelse med hendelseId={} mangler nyeste FNR eller NPID i PDL. Ignorerer hendelse.", doedsfall.hendelseId());
					return;
				}

				int antallRaderOppdatert = sakRepository.oppdaterFoedselsnummerOgDoedsdatoForAktoerIder(identer.aktoerIder(), nyesteFnrEllerNpid, doedsfall.doedsdato());
				loggDatabaseoppdatering(antallRaderOppdatert, doedsfall);
			}
			case ANNULLERT -> {
				int antallRaderOppdatert = sakRepository.annullerDoedsdatoForAktoerIder(doedsfall.aktoerIder());
				loggDatabaseoppdatering(antallRaderOppdatert, doedsfall);
			}
		}
	}

	private void loggDatabaseoppdatering(int antallOppdaterteRader, Doedsfallhendelse doedsfall) {
		if (antallOppdaterteRader == 0) {
			log.info("Ingen saksrader ble oppdatert med dødsdato og fødselsnummer for hendelseId={}.", doedsfall.hendelseId());
		} else {
			log.info("Har oppdatert {} saksrader med dødsdato og fødselsnummer for hendelseId={}.", antallOppdaterteRader, doedsfall.hendelseId());
		}
	}

}