package no.nav.dokarkivpleie.consumers.pdl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.ToString;

import java.util.List;

import static no.nav.dokarkivpleie.consumers.pdl.PdlHentIdenterResponse.PdlGruppe.AKTORID;
import static no.nav.dokarkivpleie.consumers.pdl.PdlHentIdenterResponse.PdlGruppe.FOLKEREGISTERIDENT;
import static no.nav.dokarkivpleie.consumers.pdl.PdlHentIdenterResponse.PdlGruppe.NPID;

public record PdlHentIdenterResponse(
		PdlHentIdenter data,
		List<PdlError> errors
) {
	static List<String> retryCodes = List.of("server_error", "unauthenticated", "unauthorized");

	public boolean erTekniskPdlFeil() {
		return errors().stream()
				.map(PdlError::extensions)
				.map(PdlErrorExtension::code)
				.filter(java.util.Objects::nonNull)
				.anyMatch(code -> retryCodes.contains(code.toLowerCase()));
	}

	public record PdlHentIdenter(PdlIdenter hentIdenter) {
	}

	public record PdlIdenter(List<PdlIdent> identer) {

		public String nyesteFnrEllerNpid() {
			return identer.stream()
					.filter(ident -> FOLKEREGISTERIDENT == ident.gruppe())
					.filter(ident -> !ident.historisk())
					.findFirst()
					.map(PdlIdent::ident)
					.orElse(nyesteNPID());
		}

		private String nyesteNPID() {
			return identer.stream()
					.filter(ident -> NPID == ident.gruppe())
					.filter(ident -> !ident.historisk())
					.findFirst()
					.map(PdlIdent::ident)
					.orElse(null);
		}

		public List<String> aktoerIder() {
			return identer.stream()
					.filter(ident -> ident.gruppe() == AKTORID)
					.map(PdlIdent::ident)
					.toList();
		}
	}

	public record PdlIdent(
			@ToString.Exclude
			String ident,
			PdlGruppe gruppe,
			boolean historisk
	) {
	}

	@JsonIgnoreProperties({"locations", "path"})
	public record PdlError(
			String message,
			PdlErrorExtension extensions) {
	}

	public record PdlErrorExtension(
			String id,
			String code,
			String classification,
			Details details) {
	}

	public record Details(
			String type,
			String cause,
			String policy) {
	}

	public enum PdlGruppe {
		FOLKEREGISTERIDENT,
		AKTORID,
		NPID
	}


}