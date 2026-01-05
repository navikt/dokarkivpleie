package no.nav.dokarkivpleie.repository;

import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

@JdbcTest
@ContextConfiguration(classes = SlettebestillingJdbcRepository.class)
class SlettebestillingJdbcRepositoryTest {

	private static final String BEVARINGSTID_10_AAR_ETTER_BRUKERS_DOED = "10_AAR_ETTER_BRUKERS_DOED";

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	protected SlettebestillingJdbcRepository slettebestillingJdbcRepository;

	@Test
	void skalLageSlettebestillingForArkivsak() {
		List<Long> sakerIArkivsak = List.of(123L, 456L);

		slettebestillingJdbcRepository.lagSlettebestillingForArkivsak(sakerIArkivsak, BEVARINGSTID_10_AAR_ETTER_BRUKERS_DOED);

		List<Slettebestilling> slettebestillinger = namedParameterJdbcTemplate.query(
				"SELECT * FROM joark.T_SLETTEBESTILLING",
				Collections.emptyMap(),
				(rs, _) -> mapRowToSlettebestilling(rs)
		);

		assertThat(slettebestillinger).hasSize(2).map(Slettebestilling::sakId).containsExactlyInAnyOrder("123", "456");
		assertThat(slettebestillinger).extracting(
						Slettebestilling::opprettetKildeNavn,
						Slettebestilling::opprettetAvNavn,
						Slettebestilling::opprettetAv,
						Slettebestilling::slettebestillingStatus,
						Slettebestilling::slettebestillingType,
						Slettebestilling::slettebestillingHjemmel,
						Slettebestilling::slettebestillingArsak,
						Slettebestilling::begrunnelse
				)
				.allMatch(tuple -> tuple.equals(tuple("dokarkivpleie", "MerkSakerBevaringstidPassert", "MerkSakerBevaringstidPassert",
								"OPPRETTET", "DOKUMENTER_PA_SAK", "ARK", "BEVARINGSTID", BEVARINGSTID_10_AAR_ETTER_BRUKERS_DOED))
				);

		assertThat(slettebestillinger)
				.allSatisfy(s -> {
					assertThat(s.datoOpprettet()).isCloseTo(LocalDateTime.now(), within(10, SECONDS));
					assertThat(s.datoUtfores()).isEqualTo(LocalDate.now().plusDays(365));
				});
	}

	private Slettebestilling mapRowToSlettebestilling(ResultSet rs) throws SQLException {
		return Slettebestilling.builder()
				.sakId(rs.getString("SAK_ID"))
				.opprettetKildeNavn(rs.getString("OPPRETTET_KILDE_NAVN"))
				.opprettetAvNavn(rs.getString("OPPRETTET_AV_NAVN"))
				.opprettetAv(rs.getString("OPPRETTET_AV"))
				.slettebestillingStatus(rs.getString("K_SLETTEBESTILLING_STATUS"))
				.slettebestillingType(rs.getString("K_SLETTEBESTILLING_TYPE"))
				.slettebestillingHjemmel(rs.getString("K_SLETTEBESTILLING_HJEMMEL"))
				.slettebestillingArsak(rs.getString("K_SLETTEBESTILLING_ARSAK"))
				.datoUtfores(rs.getDate("DATO_UTFORES") != null ? rs.getDate("DATO_UTFORES").toLocalDate() : null)
				.datoOpprettet(rs.getTimestamp("DATO_OPPRETTET") != null ? rs.getTimestamp("DATO_OPPRETTET").toLocalDateTime() : null)
				.begrunnelse(rs.getString("BEGRUNNELSE"))
				.build();
	}

	@Builder
	private record Slettebestilling(
			String sakId,
			String opprettetKildeNavn,
			String opprettetAvNavn,
			String opprettetAv,
			String slettebestillingStatus,
			String slettebestillingType,
			String slettebestillingHjemmel,
			String slettebestillingArsak,
			LocalDate datoUtfores,
			LocalDateTime datoOpprettet,
			String begrunnelse
	) {
	}
}