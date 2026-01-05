package no.nav.dokarkivpleie.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.dokarkivpleie.repository.SqlQueries.SLETTEBESTILLING_SQL;

@Repository
public class SlettebestillingJdbcRepository {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public SlettebestillingJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void lagSlettebestillingForArkivsak(List<Long> sakIds, String begrunnelse) {
		SqlParameterSource[] batchParams = sakIds.stream()
				.map(sakId -> new MapSqlParameterSource()
						.addValue("sakId", sakId)
						.addValue("opprettetKildeNavn", "dokarkivpleie")
						.addValue("opprettetAvNavn", "MerkSakerBevaringstidPassert")
						.addValue("opprettetAv", "MerkSakerBevaringstidPassert")
						.addValue("slettebestillingStatus", "OPPRETTET")
						.addValue("slettebestillingType", "DOKUMENTER_PA_SAK")
						.addValue("slettebestillingHjemmel", "ARK")
						.addValue("slettebestillingArsak", "BEVARINGSTID")
						.addValue("datoUtfores", LocalDate.now().plusDays(365))
						.addValue("datoOpprettet", LocalDateTime.now())
						.addValue("begrunnelse", begrunnelse))
				.toArray(SqlParameterSource[]::new);

		namedParameterJdbcTemplate.batchUpdate(SLETTEBESTILLING_SQL, batchParams);
	}

}