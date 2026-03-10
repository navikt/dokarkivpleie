package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Journalpost;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.dokarkivpleie.repository.SqlQueries.HENT_JOURNALPOSTER_FOR_SAKER;

@Repository
public class JournalpostJdbcRepository {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public JournalpostJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public List<Journalpost> hentJournalposterForSaker(List<Long> sakIds) {
		SqlParameterSource params = new MapSqlParameterSource("sakIds", sakIds);

		return namedParameterJdbcTemplate.query(HENT_JOURNALPOSTER_FOR_SAKER, params, new JournalpostRowMapper());
	}

	// Merk at journaldato og journalf_enhet kan være null i databasen
	public static class JournalpostRowMapper implements RowMapper<Journalpost> {
		@Override
		public Journalpost mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Journalpost(
					rs.getTimestamp("opprettetdato").toLocalDateTime(),
					rs.getTimestamp("journaldato") != null ? rs.getTimestamp("journaldato").toLocalDateTime().toLocalDate() : null,
					rs.getString("journalstatus"),
					rs.getString("journalfoerendeEnhet")
			);
		}
	}

}