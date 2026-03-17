package no.nav.dokarkivpleie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static no.nav.dokarkivpleie.domain.SlettebestillingArsak.BEVARINGSTID;
import static no.nav.dokarkivpleie.domain.SlettebestillingHjemmel.ARK;
import static no.nav.dokarkivpleie.domain.SlettebestillingStatus.OPPRETTET;
import static no.nav.dokarkivpleie.domain.SlettebestillingType.DOKUMENTER_PA_SAK;

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

	private static final int LARGE_LENGTH = 512;
	private static final String SLETTEBESTILLING_SEQUENCE = "slettebestilling_seq";
	private static final String DATABASE_SLETTEBESTILLING_SEQUENCE = "t_slettebestilling_seq";

	private static final String MERK_SAKER_BEVARINGSTID_PASSERT = "MerkSakerBevaringstidPassert";
	private static final String DOKARKIVPLEIE = "dokarkivpleie";

	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = SLETTEBESTILLING_SEQUENCE)
	@SequenceGenerator(name = SLETTEBESTILLING_SEQUENCE, sequenceName = DATABASE_SLETTEBESTILLING_SEQUENCE, allocationSize = 1)
	@Column(name = "slettebestilling_id", nullable = false)
	private long slettebestillingId;

	@Enumerated(STRING)
	@Column(name = "k_slettebestilling_type", nullable = false)
	private SlettebestillingType slettebestillingType;

	@Enumerated(STRING)
	@Column(name = "k_slettebestilling_status", nullable = false)
	private SlettebestillingStatus slettebestillingStatus;

	@Enumerated(STRING)
	@Column(name = "k_slettebestilling_hjemmel", nullable = false)
	private SlettebestillingHjemmel slettebestillingHjemmel;

	@Enumerated(STRING)
	@Column(name = "k_slettebestilling_arsak", nullable = false)
	private SlettebestillingArsak slettebestillingArsak;

	@Column(name = "begrunnelse", length = LARGE_LENGTH)
	private String begrunnelse;

	@Column(name = "sak_id")
	private Long sakId;

	@Column(name = "dato_utfores", nullable = false)
	private LocalDate datoUtfores;

	@Column(name = "dato_opprettet", nullable = false)
	private LocalDateTime datoOpprettet;

	@Column(name = "opprettet_av", length = LARGE_LENGTH, nullable = false)
	private String opprettetAv;

	@Column(name = "opprettet_av_navn", length = LARGE_LENGTH, nullable = false)
	private String opprettetAvNavn;

	@Column(name = "opprettet_kilde_navn", length = LARGE_LENGTH, nullable = false)
	private String opprettetKildeNavn;

	public static Slettebestilling lagSlettebestilling(Long sakId, String begrunnelse) {
		return Slettebestilling.builder()
				.sakId(sakId)
				.begrunnelse(begrunnelse)
				.slettebestillingType(DOKUMENTER_PA_SAK)
				.slettebestillingStatus(OPPRETTET)
				.slettebestillingHjemmel(ARK)
				.slettebestillingArsak(BEVARINGSTID)
				.datoUtfores(LocalDate.now().plusDays(365))
				.datoOpprettet(LocalDateTime.now())
				.opprettetAv(MERK_SAKER_BEVARINGSTID_PASSERT)
				.opprettetAvNavn(MERK_SAKER_BEVARINGSTID_PASSERT)
				.opprettetKildeNavn(DOKARKIVPLEIE)
				.build();
	}

}