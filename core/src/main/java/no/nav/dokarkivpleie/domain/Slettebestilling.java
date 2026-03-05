package no.nav.dokarkivpleie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entitet for slettesbestilling av dokumenter.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_SLETTEBESTILLING")
public class Slettebestilling {

	@Id
	//TODO: Undersøk sequences
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "slettebestilling_seq")
	@SequenceGenerator(name = "slettebestilling_seq", sequenceName = "joark.t_slettebestilling_seq", allocationSize = 1)
	@Column(name = "slettebestilling_id")
	private Long slettebestillingId;

	@Column(name = "sak_id")
	private Long sakId;

	@Column(name = "opprettet_kilde_navn")
	private String opprettetKildeNavn;

	@Column(name = "opprettet_av_navn")
	private String opprettetAvNavn;

	@Column(name = "opprettet_av")
	private String opprettetAv;

	@Column(name = "k_slettebestilling_status")
	private String slettebestillingStatus;

	@Column(name = "k_slettebestilling_type")
	private String slettebestillingType;

	@Column(name = "k_slettebestilling_hjemmel")
	private String slettebestillingHjemmel;

	@Column(name = "k_slettebestilling_arsak")
	private String slettebestillingArsak;

	@Column(name = "dato_utfores")
	private LocalDate datoUtfores;

	@Column(name = "dato_opprettet")
	private LocalDateTime datoOpprettet;

	@Column(name = "begrunnelse")
	private String begrunnelse;
}
