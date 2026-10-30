package no.nav.dokarkivpleie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.ARKIVERT;
import static no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand.FEILET;

/// Opplasting av en dokumentfil til dokarkivmellomlager. Når tilstand er `ARKIVERING` skal filen arkiveres i [DokumentFil].
@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_DOKUMENT_FIL_OPPLASTING")
public class DokumentFilOpplasting {

	@Id
	@Column(name = "dokument_fil_id", nullable = false, length = 128)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID dokumentFilId;

	@Column(name = "ekstern_dokument_referanse_id", nullable = false, length = 512)
	private String eksternDokumentReferanseId;

	@Column(name = "sha256_sjekksum", nullable = false)
	private byte[] sha256Sjekksum;

	@Column(name = "crc32c_sjekksum")
	private byte[] crc32cSjekksum;

	@Column(name = "media_type", nullable = false, length = 128)
	private String mediaType;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "tilstand", nullable = false, length = 128)
	private DokumentFilOpplastingTilstand tilstand;

	@Column(name = "opprettet_kilde_navn", nullable = false, length = 512)
	private String opprettetKildeNavn;

	@CreationTimestamp
	@Column(name = "dato_opprettet", nullable = false)
	private LocalDateTime datoOpprettet;

	@Column(name = "endret_kilde_navn", length = 512)
	private String endretKildeNavn;

	@UpdateTimestamp
	@Column(name = "dato_endret")
	private LocalDateTime datoEndret;

	@Column(name = "dato_klient_lease", nullable = false)
	private LocalDateTime datoKlientLease;

	@Version
	@Column(name = "versjon", nullable = false)
	private int versjon;

	@Column(name = "antall_bytes")
	private Long antallBytes;

	@Column(name = "dato_lastet_opp")
	private LocalDateTime datoLastetOpp;

	@Column(name = "dato_arkivert")
	private LocalDateTime datoArkivert;

	@Column(name = "forsok", nullable = false)
	private int forsok;

	public void arkivert(String kildeNavn) {
		LocalDateTime naa = LocalDateTime.now();
		this.tilstand = ARKIVERT;
		this.datoArkivert = naa;
		endret(kildeNavn, naa);
	}

	public void feilet(String kildeNavn) {
		this.tilstand = FEILET;
		endret(kildeNavn, LocalDateTime.now());
	}

	/// Øker antall forsøk. Setter tilstand til `FEILET` dersom `maksForsok` er nådd.
	public void registrerFeiletForsok(int maksForsok, String kildeNavn) {
		this.forsok++;
		if (this.forsok >= maksForsok) {
			this.tilstand = FEILET;
		}
		endret(kildeNavn, LocalDateTime.now());
	}

	private void endret(String kildeNavn, LocalDateTime tidspunkt) {
		this.endretKildeNavn = kildeNavn;
		this.datoEndret = tidspunkt;
	}
}
