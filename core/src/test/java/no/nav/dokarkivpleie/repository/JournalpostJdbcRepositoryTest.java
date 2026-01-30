package no.nav.dokarkivpleie.repository;

import no.nav.dokarkivpleie.domain.Journalpost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@JdbcTest
@ContextConfiguration(classes = JournalpostJdbcRepository.class)
public class JournalpostJdbcRepositoryTest {

	public static final LocalDateTime OPPRETTETDATO_JP_123 = LocalDateTime.parse("2025-01-01T13:30:00");
	private static final LocalDate JOURNALDATO_JP_123 = LocalDate.parse("2025-01-02");

	private static final LocalDateTime OPPRETTETDATO_JP_234 = LocalDateTime.parse("2025-02-13T14:45:00");
	private static final LocalDate JOURNALDATO_JP_234 = LocalDate.parse("2025-02-13");

	@Autowired
	protected JournalpostJdbcRepository journalpostJdbcRepository;

	@Test
	public void skalHenteJournalposterForSaker() {
		List<Journalpost> journalposter = journalpostJdbcRepository.hentJournalposterForSaker(List.of(123L, 234L));

		assertThat(journalposter)
				.extracting(Journalpost::opprettetdato, Journalpost::journaldato, Journalpost::journalstatus, Journalpost::journalfoerendeEnhet)
				.containsExactlyInAnyOrder(
						tuple(OPPRETTETDATO_JP_123, JOURNALDATO_JP_123, "FL", "1234"),
						tuple(OPPRETTETDATO_JP_123, JOURNALDATO_JP_123, "E", "1234"),
						tuple(OPPRETTETDATO_JP_234, JOURNALDATO_JP_234, "FS", "5678")
				);
	}

	@Test
	public void skalIkkeHenteFeilregistrerteJournalposterForSaker() {
		List<Journalpost> journalposter = journalpostJdbcRepository.hentJournalposterForSaker(List.of(456L));

		assertThat(journalposter).isEmpty();
	}

}