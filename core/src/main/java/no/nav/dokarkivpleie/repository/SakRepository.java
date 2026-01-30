package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Sak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// Vurder å ta i bruk noe à la BaseJpaRepository (fra dokarkiv) for å begrense rettigheter til deleteAll etc.
public interface SakRepository extends JpaRepository<Sak, Long> {

	@Query(value = """ 
		select distinct(sak.aktoerId) from Sak sak
		where sak.tema = :fagomraade
			and sak.aktoerId is not null
			and sak.kassasjonsstatus is null
			and sak.doedsdato is null
	""")
	Set<String> finnAktoeriderDerFoedselsnummerOgDoedsdatoSkalBliOppdatert(String fagomraade);

	@Modifying
	@Query("""
		update Sak sak
		set sak.brukerIdType = 'FNR',
			sak.brukerId = :foedselsnummer,
		    sak.doedsdato = :doedsdato,
			sak.endretAv = 'MerkSakerBevaringstidPassert',
			sak.datoEndret = current_timestamp
		where sak.aktoerId = :aktoerId
			and (sak.saksstatus is null or sak.saksstatus = 'AAPEN')
	""")
	void oppdaterFoedselsnummerOgDoedsdato(String aktoerId, String foedselsnummer, LocalDate doedsdato);

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