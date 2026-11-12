package no.nav.dokarkivpleie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.GenerationType.SEQUENCE;

/// Arkivert dokumentfil i Joark.
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_DOKUMENT_FIL")
public class DokumentFil {

	private static final String DOKUMENT_FIL_SEQUENCE = "dokument_fil_seq";
	private static final String DATABASE_DOKUMENT_FIL_SEQUENCE = "T_DOKUMENT_FIL_SEQ";

	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = DOKUMENT_FIL_SEQUENCE)
	@SequenceGenerator(name = DOKUMENT_FIL_SEQUENCE, sequenceName = DATABASE_DOKUMENT_FIL_SEQUENCE, allocationSize = 1)
	@Column(name = "dokument_fil_id", nullable = false)
	private Long dokumentFilId;

	@Column(name = "fil_uuid", nullable = false, length = 36)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID filUuid;

	@Lob
	@Column(name = "fil", nullable = false)
	private Blob fil;

	@CreationTimestamp
	@Column(name = "dato_opprettet", nullable = false)
	private LocalDateTime datoOpprettet;

	@Column(name = "opprettet_av", nullable = false, length = 40)
	private String opprettetAv;

	@UpdateTimestamp
	@Column(name = "dato_endret")
	private LocalDateTime datoEndret;

	@Column(name = "endret_av", length = 40)
	private String endretAv;

	@Version
	@Column(name = "versjon", nullable = false)
	private long versjon;

	@Column(name = "opprettet_kilde_navn", nullable = false, length = 40)
	private String opprettetKildeNavn;

	@Column(name = "endret_kilde_navn", length = 40)
	private String endretKildeNavn;
}
