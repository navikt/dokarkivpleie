package no.nav.dokarkivpleie.repository;

import jakarta.persistence.QueryHint;
import no.nav.dokarkivpleie.domain.DokumentFilOpplasting;
import no.nav.dokarkivpleie.domain.DokumentFilOpplastingTilstand;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface DokumentFilOpplastingRepository extends BaseJpaRepository<DokumentFilOpplasting, UUID>, HibernateRepository<DokumentFilOpplasting> {

	/// Hibernate tolker lock timeout -2 som `SKIP LOCKED` (`org.hibernate.Timeouts.SKIP_LOCKED_MILLI`).
	String SKIP_LOCKED = "-2";

	@Query("""
			select o.dokumentFilId
			from DokumentFilOpplasting o
			where o.tilstand = :tilstand
			order by o.dokumentFilId
			""")
	List<UUID> findDokumentFilIdByTilstand(DokumentFilOpplastingTilstand tilstand, Limit limit);

	/// Låser raden med `for update skip locked`. Tom dersom raden er låst av en annen transaksjon eller har en annen tilstand.
	@Lock(PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
	@Query("""
			select o
			from DokumentFilOpplasting o
			where o.dokumentFilId = :dokumentFilId
				and o.tilstand = :tilstand
			""")
	Optional<DokumentFilOpplasting> findAndLockByDokumentFilIdAndTilstand(UUID dokumentFilId, DokumentFilOpplastingTilstand tilstand);
}
