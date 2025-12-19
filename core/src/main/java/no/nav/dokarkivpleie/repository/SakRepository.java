package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Sak;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

// Vurder å ta i bruk noe à la BaseJpaRepository (fra dokarkiv) for å begrense rettigheter til deleteAll etc.
public interface SakRepository extends CrudRepository<Sak, Long> {

	@Query(value = """ 
		select distinct(sak.aktoerId) from Sak sak
		where sak.tema = :fagomraade
			and sak.aktoerId is not null
			and sak.kassasjonsstatus is null
			and sak.doedsdato is null
	""")
	List<String> finnAktoeriderDerFoedselsnummerOgDoedsdatoSkalBliOppdatert(String fagomraade);

}