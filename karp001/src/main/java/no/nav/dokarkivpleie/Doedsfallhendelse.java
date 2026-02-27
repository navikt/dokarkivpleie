package no.nav.dokarkivpleie;

import lombok.Builder;
import no.nav.dokarkivpleie.domain.leesah.Endringstype;

import java.time.LocalDate;
import java.util.List;

@Builder
public record Doedsfallhendelse(
		String hendelseId,
		LocalDate doedsdato,
		Endringstype endringstype,
		List<String> personidenter) {

	String foerstePersonident() {
		return personidenter().getFirst();
	}

	List<String> aktoerIder() {
		return personidenter.stream()
				.filter(ident -> ident.length() == 13)
				.toList();
	}

}