package no.nav.dokarkivpleie.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

import static no.nav.dokarkivpleie.repository.SqlQueries.HENT_NAVN_FOR_ADMINISTRATIV_ENHET;

@Repository
public class AdministrativEnhetJdbcRepository {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AdministrativEnhetJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public String hentNavnForAdministrativEnhet(String fagomraade, LocalDateTime opprettetTidspunkt) {
		SqlParameterSource params = new MapSqlParameterSource(Map.of(
				"fagomraade", fagomraade,
				"opprettet_tidspunkt", opprettetTidspunkt.toLocalDate())
		);

		try {
			return namedParameterJdbcTemplate.queryForObject(HENT_NAVN_FOR_ADMINISTRATIV_ENHET, params, String.class);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}

	}

}