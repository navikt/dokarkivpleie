package no.nav.dokarkivpleie.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ContextConfiguration(classes = AdministrativEnhetJdbcRepository.class)
class AdministrativEnhetJdbcRepositoryTest {

	@Autowired
	AdministrativEnhetJdbcRepository administrativEnhetJdbcRepository;

	@Test
	void skalHenteAdministrativEnhet() {
		String enhetsnavn = administrativEnhetJdbcRepository.hentNavnForAdministrativEnhet("UFM", LocalDateTime.parse("2015-06-12T12:00:00"));

		assertThat(enhetsnavn).isEqualTo("Nav Internasjonalt");
	}

	@Test
	void skalReturnereNullHvisOpprettetTidspunktErFoerDatoFom() {
		var opprettetTidspunktFoerAdministrativEnhetErGyldigFra = LocalDateTime.parse("2010-06-12T12:00:00");

		String enhetsnavn = administrativEnhetJdbcRepository.hentNavnForAdministrativEnhet("UFM", opprettetTidspunktFoerAdministrativEnhetErGyldigFra);

		assertThat(enhetsnavn).isNull();
	}

	@Test
	void skalReturnereNullHvisOpprettetTidspunktErEtterDatoTom() {
		var opprettetTidspunktEtterAdministrativEnhetErGyldigTil = LocalDateTime.parse("2100-01-01T12:00:00");

		String enhetsnavn = administrativEnhetJdbcRepository.hentNavnForAdministrativEnhet("UFM", opprettetTidspunktEtterAdministrativEnhetErGyldigTil);

		assertThat(enhetsnavn).isNull();
	}

	@Test
	void skalReturnereNullHvisTemaIkkeFinnes() {
		var temaSomIkkeLiggerITabellen = "BIL";

		String enhetsnavn = administrativEnhetJdbcRepository.hentNavnForAdministrativEnhet(temaSomIkkeLiggerITabellen, LocalDateTime.parse("2015-06-12T12:00:00"));

		assertThat(enhetsnavn).isNull();
	}

}