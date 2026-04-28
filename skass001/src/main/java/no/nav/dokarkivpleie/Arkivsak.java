package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.domain.Journalpost;
import no.nav.dokarkivpleie.domain.Sak;
import no.nav.dokarkivpleie.domain.Saksstatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static no.nav.dokarkivpleie.domain.JournalpostStatus.FERDIGSTILTE_JOURNALPOSTSTATUSER;
import static no.nav.dokarkivpleie.domain.JournalpostStatus.MIDLERTIDIGE_JOURNALPOSTSTATUSER;
import static no.nav.dokarkivpleie.domain.Saksstatus.AAPEN;
import static no.nav.dokarkivpleie.domain.Saksstatus.AVBRUTT;
import static no.nav.dokarkivpleie.domain.Saksstatus.AVLEVERT;
import static no.nav.dokarkivpleie.domain.Saksstatus.AVSLUTTET;
import static org.apache.logging.log4j.util.Strings.isBlank;

public record Arkivsak(
		String fagsakNr,
		String applikasjon,
		String brukerId,
		Set<Sak> saker,
		List<Journalpost> journalposter
) {
	public Arkivsak(String fagsakNr, String applikasjon, String brukerId, Set<Sak> saker) {
		this(fagsakNr, applikasjon, brukerId, saker, new ArrayList<>());
	}

	public static final String MASKINELL_JOURNALFOERENDE_ENHET = "9999";
	public static final EnumSet<Saksstatus> LUKKEDE_STATUSER = EnumSet.of(AVBRUTT, AVSLUTTET, AVLEVERT);

	public List<Long> saksIder() {
		return saker.stream()
				.map(Sak::getSakId)
				.toList();
	}

	public Set<String> journalpoststatuser() {
		return journalposter.stream()
				.map(Journalpost::journalstatus)
				.collect(Collectors.toSet());
	}

	public boolean harBaadeAapneOgLukkedeSaker() {
		boolean inneholderAapneStatuser = saker.stream().anyMatch(sak -> sak.getSaksstatus() == null || AAPEN.equals(sak.getSaksstatus()));
		boolean inneholderLukkedeStatuser = saker.stream().anyMatch(sak ->  LUKKEDE_STATUSER.contains(sak.getSaksstatus()));

		return inneholderAapneStatuser && inneholderLukkedeStatuser;
	}

	public boolean harKunAapneSaker() {
		return saker.stream().allMatch(sak -> sak.getSaksstatus() == null || AAPEN.equals(sak.getSaksstatus()));
	}

	public boolean harIngenFerdigstilteJournalposter() {
		return journalposter.stream().noneMatch(journalpost -> FERDIGSTILTE_JOURNALPOSTSTATUSER.contains(journalpost.journalstatus()));
	}

	public boolean harJournalposterIMidlertidigeStatuser() {
		return !Collections.disjoint(journalpoststatuser(), MIDLERTIDIGE_JOURNALPOSTSTATUSER);
	}

	public Optional<Journalpost> finnEldsteFerdigstilteJournalpostMedJournalfoerendeEnhetUlikMaskinell() {
		List<Journalpost> filtrerteJournalposter = journalposter().stream()
				.filter(journalpost -> FERDIGSTILTE_JOURNALPOSTSTATUSER.contains(journalpost.journalstatus()))
				.filter(journalpost -> !isBlank(journalpost.journalfoerendeEnhet()) && !MASKINELL_JOURNALFOERENDE_ENHET.equals(journalpost.journalfoerendeEnhet()))
				.toList();

		if (filtrerteJournalposter.isEmpty()) {
			return Optional.empty();
		}

		return filtrerteJournalposter.stream()
				.min(Comparator.comparing(Journalpost::journaldato));
	}

	public LocalDateTime finnOpprettetDatoForEldsteJournalpost() {
		return journalposter.stream()
				.map(Journalpost::journaldato)
				.min(naturalOrder()).get().atStartOfDay();
	}

	public LocalDateTime finnOpprettetTidspunktForEldsteSak() {
		return saker.stream()
				.map(Sak::getOpprettetTidspunkt)
				.min(naturalOrder())
				.orElse(null);
	}

}