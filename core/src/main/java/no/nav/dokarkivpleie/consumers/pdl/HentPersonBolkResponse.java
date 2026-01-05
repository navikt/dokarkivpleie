package no.nav.dokarkivpleie.consumers.pdl;

import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

public record HentPersonBolkResponse(
		HentPersonBolkData data,
		List<Error> errors
) {

	public record HentPersonBolkData(
			List<HentPersonBolk> hentPersonBolk
	) {
	}

	public record HentPersonBolk(
			@ToString.Exclude
			// Ident vi sender inn (potensielt historisk)
			String ident,
			// ok / NOT_FOUND
			String code,
			Person person
	) {
	}

	public record Person(
			List<Doedsfall> doedsfall,
			List<Folkeregisteridentifikator> folkeregisteridentifikator
	) {
		public boolean erDoed() {
			return doedsfall != null && !doedsfall.isEmpty() && doedsfall.getFirst().doedsdato != null;
		}

		//TODO: Nullpointers?
		public String folkeregisterIdent() { return folkeregisteridentifikator.getFirst().identifikasjonsnummer; }
	}

	public record Folkeregisteridentifikator(
			// Gjeldende ident (som regel FNR)
			@ToString.Exclude
			String identifikasjonsnummer,
			String status,
			String type
	) {
	}

	public record Doedsfall(
			LocalDate doedsdato
	) {
	}

	public record Error(
			String message,
			ErrorExtension extensions
	) {
	}

	public record ErrorExtension(
			String code,
			String classification,
			Details details
	) {
	}

	public record Details(
			List<String> errors
	) {
	}
}