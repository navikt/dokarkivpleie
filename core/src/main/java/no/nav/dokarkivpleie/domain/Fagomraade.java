package no.nav.dokarkivpleie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@Getter
@AllArgsConstructor
@Table(name = "T_K_FAGOMRADE")
public class Fagomraade {

	@Id
	@Column(name = "k_fagomrade")
	private String kode;

	@Column(name = "k_bevaringstid")
	private String bevaringstid;

	@Column(name = "avlever_med_dok")
	private Boolean avleverMedDok;

	public Fagomraade() {
	}
}