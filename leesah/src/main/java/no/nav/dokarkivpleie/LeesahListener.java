package no.nav.dokarkivpleie;

import lombok.extern.slf4j.Slf4j;
import no.nav.person.pdl.leesah.Personhendelse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.dokarkivpleie.DoedsfallhendelseMapper.mapPersonhendelseTilDoedsfall;
import static no.nav.dokarkivpleie.Karp001Service.OPPLYSNINGSTYPE_DOEDSFALL;

@Slf4j
@Service
public class LeesahListener {

	private final Karp001Service karp001Service;

	public LeesahListener(Karp001Service karp001Service) {
		this.karp001Service = karp001Service;
	}

	@Transactional
	@KafkaListener(
			topics = "pdl.leesah-v1",
			groupId = "dokarkivpleie"
	)
	public void lesPersonhendelserFraLeesah(Personhendelse personhendelse) {
		if (OPPLYSNINGSTYPE_DOEDSFALL.equals(personhendelse.getOpplysningstype().toString())) {
			karp001Service.behandleDoedsfallhendelseFraLeesah(mapPersonhendelseTilDoedsfall(personhendelse));
		}
	}

}