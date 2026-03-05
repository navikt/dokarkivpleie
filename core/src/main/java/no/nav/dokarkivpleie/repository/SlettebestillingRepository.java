package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Slettebestilling;

public interface SlettebestillingRepository extends HibernateRepository<Slettebestilling>, BaseJpaRepository<Slettebestilling, Long>{
}