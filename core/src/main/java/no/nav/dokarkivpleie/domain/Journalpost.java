package no.nav.dokarkivpleie.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Journalpost(
		LocalDateTime opprettetdato,
		LocalDate journaldato,
		String journalstatus,
		String journalfoerendeEnhet
) {}