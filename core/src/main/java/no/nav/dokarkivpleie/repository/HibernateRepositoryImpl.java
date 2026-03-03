package no.nav.dokarkivpleie.repository;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Kopiert og modifisiert fra HibernateRepositoryImpl.java i https://github.com/vladmihalcea/hypersistence-utils (hypersistence-utils-hibernate-71)
 *
 * @param <T> JPA entiteten.
 */
public class HibernateRepositoryImpl<T> implements HibernateRepository<T> {

	private final EntityManager entityManager;

	public HibernateRepositoryImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public <S extends T> S persist(S entity) {
		entityManager.persist(entity);
		return entity;
	}

	public <S extends T> List<S> persistAll(Iterable<S> entities) {
		List<S> result = new ArrayList<>();
		for(S entity : entities) {
			result.add(persist(entity));
		}
		return result;
	}

}