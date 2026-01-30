package no.nav.dokarkivpleie.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.consumers.pdl.HentPersonBolkResponse.HentPersonBolk;
import no.nav.dokarkivpleie.consumers.pdl.HentPersonBolkResponse.Person;
import no.nav.dokarkivpleie.consumers.pdl.PdlConsumer;
import no.nav.dokarkivpleie.domain.Fagomraade;
import no.nav.dokarkivpleie.repository.SakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FnrOgDoedsdatoService {

	private final SakRepository sakRepository;
	private final PdlConsumer pdlConsumer;

	public FnrOgDoedsdatoService(SakRepository sakRepository, PdlConsumer pdlConsumer) {
		this.sakRepository = sakRepository;
		this.pdlConsumer = pdlConsumer;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void oppdaterFnrOgDoedsdatoForAvdoedePersoner(Fagomraade fagomraade) {
		Set<String> aktoerIder = sakRepository.finnAktoeriderDerFoedselsnummerOgDoedsdatoSkalBliOppdatert(fagomraade.getKode());

		if (aktoerIder.isEmpty()) {
			log.info("Fant ingen aktoerIder på tema={} som var aktuelle for oppdatering av fnr og dødsdato", fagomraade.getKode());
		} else {
			log.info("Har hentet {} aktoerIder på tema={} som er aktuelle for oppdatering av fnr og dødsdato", aktoerIder.size(), fagomraade.getKode());

			List<List<String>> partisjonerteAktoerIder = Lists.partition(aktoerIder.stream().toList(), 1000);
			partisjonerteAktoerIder.forEach(this::oppdaterDoedePersonerSineSakerMedNyesteFnrOgDoedsdato);

			log.info("Har oppdatert fnr og dødsdato for avdøde personer på tema={}", fagomraade.getKode());
		}
	}

	//Er det mulig å sjekke om dette allerede er gjort (eks hvis jobben stopper i neste steg og vi kjører på nytt)?
	private void oppdaterDoedePersonerSineSakerMedNyesteFnrOgDoedsdato(List<String> aktoerIder) {
		List<HentPersonBolk> personer = pdlConsumer.hentNyesteFnrOgDoedsdato(aktoerIder);
		List<HentPersonBolk> doedePersoner = personer.stream()
				.filter(hentPersonBolk -> hentPersonBolk.person().erDoed())
				.toList();

		doedePersoner.forEach(doedPersonBolk -> {
			Person doedPerson = doedPersonBolk.person();
			sakRepository.oppdaterFoedselsnummerOgDoedsdato(
					doedPersonBolk.ident(),
					doedPerson.folkeregisterIdent(),
					doedPerson.doedsfall().getFirst().doedsdato()
			);
		});
	}
}
