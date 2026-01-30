package no.nav.dokarkivpleie.consumers.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatavarehusResponse {

	List<AdministrativEnhet> items;

	@Builder
	public record AdministrativEnhet(
			@JsonProperty("mapping_node_kode")
			String journalfoerendeEnhet,
			@JsonProperty("mapping_node_type")
			String kontortype,
			@JsonProperty("nav_enhet_navn")
			String kontornavn,

			@JsonProperty("funk_gyldig_fra_dato")
			LocalDate gyldigFraDato,

			@JsonProperty("funk_gyldig_til_dato")
			LocalDate gyldigTilDato
	) {
	}

}