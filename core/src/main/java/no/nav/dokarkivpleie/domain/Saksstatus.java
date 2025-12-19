package no.nav.dokarkivpleie.domain;

/**
 * Enum for codes in T_K_SAK_STATUS.
 */
public enum Saksstatus {

	/**
	 * Saken er opprettet og åpen/aktiv. Nye journalposter kan knyttes til saken.
	 */
	AAPEN,

	/**
	 * Saken er avsluttet.
	 */
	AVSLUTTET,

	/**
	 * Saken er avlevert.
	 */
	AVLEVERT,

	/**
	 * Saken er avbrutt.
	 */
	AVBRUTT

}