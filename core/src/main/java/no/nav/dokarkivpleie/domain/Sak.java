package no.nav.dokarkivpleie.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inneholder metadata om sakstilknytningen til fagsystemet.
 */
@Entity
@Builder
@Table(name = "SAK")
@NoArgsConstructor
@AllArgsConstructor
public class Sak {

	@Id
	@Column(name = "id")
	private Long sakId;

	@Column(name = "tema")
	private String tema;

	@Column(name = "applikasjon")
	private String applikasjon;

	@Column(name = "fagsaknr")
	private String fagsakNr;

	@Column(name = "aktoerid")
	private String aktoerId;

	@Column(name = "opprettet_tidspunkt")
	private LocalDateTime opprettetTidspunkt;

	@Enumerated(EnumType.STRING)
	@Column(name = "k_sak_status")
	private Saksstatus saksstatus;

	@Column(name = "endret_av")
	private String endretAv;

	@Column(name = "dato_endret")
	private LocalDateTime datoEndret;

	@Column(name = "dato_avsluttet")
	private LocalDateTime datoAvsluttet;

	@Column(name = "endret_kilde_navn")
	private String endretKildeNavn;

	@Column(name = "avsluttet_av")
	private String avsluttetAv;

	@Column(name = "avsluttet_kilde_navn")
	private String avsluttetKildeNavn;

	@Column(name = "dato_sak_opprettet")
	private LocalDateTime datoSakOpprettet;

	@Column(name = "administrativ_enhet")
	private String administrativEnhet;

	@Column(name = "sak_ansvarlig")
	private String saksansvarlig;

	@Enumerated(EnumType.STRING)
	@Column(name = "k_kassasjon_status")
	private Kassasjonsstatus kassasjonsstatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "k_avlevering_status")
	private Avleveringsstatus avleveringsstatus;

	@Column(name = "doedsdato")
	private LocalDate doedsdato;

}