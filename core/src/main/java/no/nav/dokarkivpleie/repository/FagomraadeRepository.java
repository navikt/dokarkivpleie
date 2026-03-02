package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Fagomraade;

public interface FagomraadeRepository extends BaseJpaRepository<Fagomraade, String>, HibernateRepository<Fagomraade> {

	Fagomraade findFagomraadeByKode(String kode);

}