package no.nav.dokarkivpleie.repository;

import java.util.List;

/* Kopi av lisensfil: https://github.com/vladmihalcea/hypersistence-utils/blob/master/LICENSE
Copyright {2017-2020} {Mihalcea Vlad-Alexandru}

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Changes done to the file:
- Remove following methods:
	* findAll
	* save
	* saveAll
	* saveAndFlush
	* saveAllAndFlush
	* merge
	* update
	* persistAndFlush
	* persistAllAndFlush
	* mergeAndFlush
	* mergeAll
	* mergeAllAndFlush
	* updateAndFlush
	* updateAll
	* updateAllAndFlush
*/

/**
 * Kopiert og modifisiert fra HibernateRepository.java i https://github.com/vladmihalcea/hypersistence-utils (hypersistence-utils-hibernate-71)
 * <p>
 * Metoder for å behandle Hibernate sine entity state changes.
 *
 * @param <T> JPA entiteten.
 */
public interface HibernateRepository<T> {

	/**
	 * The persist method allows you to pass the provided entity to the {@code persist} method of the
	 * underlying JPA {@code EntityManager}.
	 *
	 * @param entity entity to persist
	 * @param <S>    entity type
	 * @return entity
	 */
	<S extends T> S persist(S entity);

	/**
	 * The persistAll method allows you to pass the provided entities to the {@code persist} method of the
	 * underlying JPA {@code EntityManager}.
	 *
	 * @param entities entities to persist
	 * @param <S>    entity type
	 * @return entities
	 */
	<S extends T> List<S> persistAll(Iterable<S> entities);

}