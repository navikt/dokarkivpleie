package no.nav.dokarkivpleie.repository;

public class SqlQueries {

	public static final String HENT_JOURNALPOSTER_FOR_SAKER = """
			select
				jp.k_journal_s as journalstatus,
				jp.journalf_enhet as journalfoerendeEnhet,
				jp.dato_journal as journaldato,
				jp.dato_opprettet as opprettetdato
			from joark.t_saksrelasjon sr
				join joark.t_journalpost jp on jp.journalpost_id = sr.journalpost_id
			where sr.sak_id in (:sakIds)
			and (sr.feilregistrert is null or sr.feilregistrert = '0')
			""";

	// TODO: Kan ein både bruke <= og >=?
	public static final String HENT_NAVN_FOR_ADMINISTRATIV_ENHET = """
			select enhet_navn
			from joark.t_administrativ_enhet
			where tema = :fagomraade
			and dato_fom <= :opprettet_tidspunkt
			and dato_tom >= :opprettet_tidspunkt
			""";

	public static final String SLETTEBESTILLING_SQL = """
			insert into joark.t_slettebestilling (
				slettebestilling_id,
				sak_id,
				opprettet_kilde_navn,
				opprettet_av_navn,
				opprettet_av,
				k_slettebestilling_status,
				k_slettebestilling_type,
				k_slettebestilling_hjemmel,
				k_slettebestilling_arsak,
				dato_utfores,
				dato_opprettet,
				begrunnelse)
			values (
				joark.t_slettebestilling_seq.nextval,
				:sakId,
				:opprettetKildeNavn,
				:opprettetAvNavn,
				:opprettetAv,
				:slettebestillingStatus,
				:slettebestillingType,
				:slettebestillingHjemmel,
				:slettebestillingArsak,
				:datoUtfores,
				:datoOpprettet,
				:begrunnelse
			)
			""";
}