package no.nav.dokarkivpleie.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Base JPA repository interface.
 * <p>
 * Inneholder metoder som er tilgjengelig for utviklere i produksjonskode.
 * Kan utvides med flere metoder som burde være felles for entitet repositories.
 *
 * @param <T>  JPA entiteten
 * @param <ID> ID datatypen
 */
@NoRepositoryBean
public interface BaseJpaRepository<T, ID> extends Repository<T, ID> {

	Optional<T> findById(ID id);

	List<T> findAllById(Iterable<ID> ids);
}