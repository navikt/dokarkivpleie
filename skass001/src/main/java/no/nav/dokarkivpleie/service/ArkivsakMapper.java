package no.nav.dokarkivpleie.service;

import no.nav.dokarkivpleie.Arkivsak;
import no.nav.dokarkivpleie.domain.Sak;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArkivsakMapper {

	public static List<Arkivsak> mapSakerTilArkivsaker(Set<Sak> saker) {
		List<Arkivsak> arkivsaker = new ArrayList<>();

		//Det finnes saker i databasen uten applikasjon, disse hopper vi over i denne omgang da de ikke kan grupperes til arkivsaker.
		List<Sak> sakerMedApplikasjon = saker.stream().filter(sak -> sak.getApplikasjon() != null).toList();
		arkivsaker.addAll(mapSakerMedFagsaknrTilArkivsaker(sakerMedApplikasjon));
		arkivsaker.addAll(mapSakerUtenFagsaknrTilArkivsaker(sakerMedApplikasjon));
		return arkivsaker;
	}

	public static List<Arkivsak> mapSakerMedFagsaknrTilArkivsaker(List<Sak> saker) {
		return saker.stream()
				.filter(sak -> sak.getFagsakNr() != null)
				.collect(Collectors.groupingBy(sak -> List.of(
						sak.getFagsakNr(),
						sak.getApplikasjon(),
						sak.getBrukerId()
				)))
				.entrySet()
				.stream()
				.map(entry -> new Arkivsak(
						entry.getKey().get(0), //fagsakNr
						entry.getKey().get(1), //applikasjon
						entry.getKey().get(2), //brukerId
						Set.copyOf(entry.getValue())
				))
				.toList();
	}

	public static List<Arkivsak> mapSakerUtenFagsaknrTilArkivsaker(List<Sak> saker) {
		return saker.stream()
				.filter(sak -> sak.getFagsakNr() == null)
				.collect(Collectors.groupingBy(sak -> List.of(
						sak.getApplikasjon(),
						sak.getBrukerId()
				)))
				.entrySet()
				.stream()
				.map(entry -> new Arkivsak(
						null, //fagsaknr
						entry.getKey().get(0), //applikasjon
						entry.getKey().get(1), //brukerId
						Set.copyOf(entry.getValue())
				))
				.toList();
	}

}