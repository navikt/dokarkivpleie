package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Fagomraade;
import org.springframework.data.repository.CrudRepository;

public interface FagomraadeRepository extends CrudRepository<Fagomraade, String> {

	Fagomraade findFagomraadeByKode(String kode);

}