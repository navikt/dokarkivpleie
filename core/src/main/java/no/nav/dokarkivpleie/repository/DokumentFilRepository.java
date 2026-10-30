package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.DokumentFil;

import java.util.Optional;
import java.util.UUID;

public interface DokumentFilRepository extends BaseJpaRepository<DokumentFil, Long>, HibernateRepository<DokumentFil> {
	boolean existsByFilUuid(UUID filUuid);

	Optional<DokumentFil> findByFilUuid(UUID filUuid);
}
