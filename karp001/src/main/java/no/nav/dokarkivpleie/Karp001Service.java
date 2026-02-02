package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.consumers.pdl.PdlConsumer;
import no.nav.dokarkivpleie.consumers.pdl.PdlHentIdenterResponse.PdlIdenter;
import no.nav.dokarkivpleie.repository.SakRepository;
import no.nav.person.pdl.leesah.Personhendelse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static no.nav.dokarkivpleie.PersonhendelseValidator.validerPersonhendelse;

@Slf4j
@Service
public class Karp001Service {

	private static final String OPPLYSNINGSTYPE_DOEDSFALL = "DOEDSFALL_V1";
	private final SakRepository sakRepository;
	private final PdlConsumer pdlConsumer;

	public Karp001Service(SakRepository sakRepository, PdlConsumer pdlConsumer) {
		this.sakRepository = sakRepository;
		this.pdlConsumer = pdlConsumer;
	}

	@Transactional
	@KafkaListener(
			topics = "pdl.leesah-v1",
			groupId = "dokarkivpleie-karp001"
	)
	public void lesPersonhendelserFraLeesah(Personhendelse personhendelse) {
		if (!erHendelseEtDoedsfall(personhendelse)) {
			loggIgnorertHendelse(personhendelse);
			return;
		}

		log.info("Hendelse med opplysningstype={}, endringstype={}, hendelseId={} mottatt.", personhendelse.getOpplysningstype(), personhendelse.getEndringstype(), personhendelse.getHendelseId());
		validerPersonhendelse(personhendelse);

		switch (personhendelse.getEndringstype()) {
			case OPPRETTET, KORRIGERT -> {
				PdlIdenter identer = pdlConsumer.hentAlleIdenterForIdent(hentFoersteIdentFraPersonhendelse(personhendelse));

				String nyesteFnr  = identer.nyesteFnr();
				if (nyesteFnr == null) {
					log.info("Hendelse med opplysningstype={} og hendelseId={} mangler nyeste FNR i PDL. Dette gjelder som regel personer med NPID. Ignorerer hendelse.", personhendelse.getOpplysningstype(), personhendelse.getHendelseId());
					return;
				}

				int antallRaderOppdatert = sakRepository.oppdaterFoedselsnummerOgDoedsdatoForAktoerIder(identer.aktoerIder(), nyesteFnr, personhendelse.getDoedsfall().getDoedsdato());
				loggDatabaseoppdatering(antallRaderOppdatert, personhendelse);
			}
			case ANNULLERT -> {
				List<String> identer = personhendelse.getPersonidenter().stream().map(CharSequence::toString).toList();

				int antallRaderOppdatert = sakRepository.annullerDoedsdatoForAktoerIder(identer);
				loggDatabaseoppdatering(antallRaderOppdatert, personhendelse);
			}
		}
	}

	private boolean erHendelseEtDoedsfall(Personhendelse personhendelse) {
		return OPPLYSNINGSTYPE_DOEDSFALL.contentEquals(personhendelse.getOpplysningstype());
	}

	String hentFoersteIdentFraPersonhendelse(Personhendelse personhendelse) {
		return personhendelse.getPersonidenter().getFirst().toString();
	}

	private void loggIgnorertHendelse(Personhendelse personhendelse) {
		log.info("Hendelse med opplysningstype={}, endringstype={}, hendelseId={} mottatt. Ignorerer hendelse.",
				personhendelse.getOpplysningstype(), personhendelse.getEndringstype(), personhendelse.getHendelseId());
	}

	private void loggDatabaseoppdatering(int antallOppdaterteRader, Personhendelse personhendelse) {
		if (antallOppdaterteRader == 0) {
			log.info("Ingen saksrader ble oppdatert med dødsdato og fødselsnummer for hendelseId={}.", personhendelse.getHendelseId());
		} else {
			log.info("Har oppdatert {} saksrader med dødsdato og fødselsnummer for hendelseId={}.", antallOppdaterteRader, personhendelse.getHendelseId());
		}
	}

}