package no.nav.dokarkivpleie.domain;

import java.util.Set;

public enum JournalpostStatus {
	J,
	M,
	U,
	D,
	R,
	FS,
	FL,
	E,
	A,
	MO,
	UB,
	OD;

	public static final Set<String> MIDLERTIDIGE_JOURNALPOSTSTATUSER = Set.of(R.name(), D.name(), M.name(), MO.name(), OD.name());

	public static final Set<String> FERDIGSTILTE_JOURNALPOSTSTATUSER = Set.of(FL.name(), FS.name(), E.name(), J.name());
}
