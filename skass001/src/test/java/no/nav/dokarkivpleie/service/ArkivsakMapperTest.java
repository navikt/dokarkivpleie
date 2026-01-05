package no.nav.dokarkivpleie.service;

import no.nav.dokarkivpleie.Arkivsak;
import no.nav.dokarkivpleie.domain.Sak;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static no.nav.dokarkivpleie.service.ArkivsakMapper.mapSakerTilArkivsaker;
import static org.assertj.core.api.Assertions.assertThat;

class ArkivsakMapperTest {

	private static final String AKTOERID_1 = "12345678901";
	private static final String AKTOERID_2 = "23456789012";
	private static final String AKTOERID_3 = "34567890123";

	private static final String BRUKERID_1 = "07417813777";
	private static final String BRUKERID_2 = "16428806054";
	private static final String BRUKERID_3 = "19477931297";

	private static final String APPLIKASJON_UTEN_FAGSAKNR = "FS22";
	private static final String APPLIKASJON_MED_FAGSAKNR = "AO01";

	@Test
	void skalMappeArkivsakerMedApplikasjon() {
		Set<Sak> saker = Set.of(
				lagSakMedAktoerId(1L, AKTOERID_1, BRUKERID_1, APPLIKASJON_UTEN_FAGSAKNR, null),
				lagSakMedAktoerId(2L, AKTOERID_2, BRUKERID_2, APPLIKASJON_UTEN_FAGSAKNR, null),
				lagSakMedAktoerId(3L, AKTOERID_1, BRUKERID_1, APPLIKASJON_MED_FAGSAKNR, "2222"),
				lagSakMedAktoerId(4L, AKTOERID_1, BRUKERID_1, APPLIKASJON_MED_FAGSAKNR, "3333"),
				lagSakMedAktoerId(5L, AKTOERID_3, BRUKERID_3, APPLIKASJON_UTEN_FAGSAKNR, null),
				lagSakMedAktoerId(6L, AKTOERID_2, BRUKERID_2, APPLIKASJON_UTEN_FAGSAKNR, null),
				lagSakMedAktoerId(7L, AKTOERID_2, BRUKERID_2, APPLIKASJON_MED_FAGSAKNR, "1234"),
				lagSakMedAktoerId(8L, AKTOERID_2, BRUKERID_2, APPLIKASJON_MED_FAGSAKNR, "2345"),
				lagSakMedAktoerId(9L, AKTOERID_2, BRUKERID_2, APPLIKASJON_MED_FAGSAKNR, "1234"),
				lagSakMedAktoerId(10L, AKTOERID_1, BRUKERID_1, APPLIKASJON_MED_FAGSAKNR, "3333"),
				lagSakMedAktoerId(11L, AKTOERID_1, BRUKERID_1, APPLIKASJON_MED_FAGSAKNR, "3333"),
				lagSakMedAktoerId(12L, AKTOERID_3, BRUKERID_3, APPLIKASJON_MED_FAGSAKNR, "2345")
		);

		List<Arkivsak> arkivsaker = mapSakerTilArkivsaker(saker);

		var forventedeSakerIArkivsakene = List.of(
				List.of(1L),
				List.of(2L, 6L),
				List.of(3L),
				List.of(4L, 10L, 11L),
				List.of(5L),
				List.of(7L, 9L),
				List.of(8L),
				List.of(12L)
		);

		assertThat(arkivsaker).hasSize(8);
		assertThat(arkivsaker.stream().map(Arkivsak::saksIder))
				.usingRecursiveComparison()
				.ignoringCollectionOrder()
				.isEqualTo(forventedeSakerIArkivsakene);
	}

	@Test
	void skalSeVekkFraSakerUtenApplikasjon() {
		Set<Sak> saker = Set.of(
				lagSakMedAktoerId(13L, AKTOERID_3, BRUKERID_3, null, null)
		);

		List<Arkivsak> arkivsaker = mapSakerTilArkivsaker(saker);

		assertThat(arkivsaker).isEmpty();
	}

	private Sak lagSakMedAktoerId(Long sakId, String aktoerId, String brukerId, String applikasjon, String fagsakNr) {
		return Sak.builder()
				.sakId(sakId)
				.applikasjon(applikasjon)
				.fagsakNr(fagsakNr)
				.aktoerId(aktoerId)
				.brukerId(brukerId)
				.build();
	}

}