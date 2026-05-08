package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Sak;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface SakRepository extends BaseJpaRepository<Sak, Long>, HibernateRepository<Sak> {

	@Modifying
	@Query("""
		update Sak sak
		set sak.brukerIdType = 'FNR',
			sak.brukerId = :foedselsnummer,
		    sak.doedsdato = :doedsdato,
			sak.endretAv = 'OppdaterSakerMedDoedsdatoOgFnr',
			sak.endretKildeNavn = 'dokarkivpleie',
			sak.datoEndret = current_timestamp
		where sak.aktoerId in (:aktoerIder)
	""")
	int oppdaterFoedselsnummerOgDoedsdatoForAktoerIder(List<String> aktoerIder, String foedselsnummer, LocalDate doedsdato);

	@Modifying
	@Query("""
		update Sak sak
		set sak.doedsdato = null,
			sak.endretAv = 'OppdaterSakerMedDoedsdatoOgFnr',
			sak.endretKildeNavn = 'dokarkivpleie',
			sak.datoEndret = current_timestamp
		where sak.aktoerId in (:aktoerIder)
			and sak.doedsdato is not null
	""")
	int annullerDoedsdatoForAktoerIder(List<String> aktoerIder);

	@Query("""
		select distinct sak.brukerId from Sak sak
		where sak.tema = :tema
			and sak.doedsdato is not null
			and current_timestamp > function('add_months', sak.doedsdato, :antallmaaneder)
			and sak.kassasjonsstatus is null
	""")
	Set<String> findDistinctFnrHvorPersonErDoedIMerEnnMaaneder(String tema, int antallmaaneder);

	@Query("""
		select sak
		from Sak sak
		where sak.tema = :tema
			and sak.brukerId in :brukerId
			and sak.kassasjonsstatus is null
	""")
	Set<Sak> finnUkasserteSakerForBrukere(List<String> brukerId, String tema);

}