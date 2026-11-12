# Dokarkivpleie

Funksjonalitet for arkivpleie i Joark.

Appen ligger per desember 2025 i FSS (fagsystemsonen), men skal bli flyttet til GCP (Google Cloud Platform) når Oracle-onprem er tilgjengelig fra GCP-clusteret. 

### Periodiske jobber:
- skass001 - [MerkSakerBevaringstidPassert](https://confluence.adeo.no/spaces/BOA/pages/494765936/SKASS001+-+MerkSakerBevaringstidPassert)
- arkiverdokumentfiler - [ArkiverDokumentFiler](https://confluence.adeo.no/spaces/BOA/pages/876651534/ArkiverDokumentFiler)

### Kafka-tjenester:
- karp001 - [BehandleDødsfallhendelse](https://confluence.adeo.no/spaces/BOA/pages/780356424/KARP001+-+BehandleDødsfallhendelse)

## Komme i gang
Prosjektet bruker Java 25. For å bygge prosjektet lokalt, kjør kommandoen:
`mvn clean install`

Appen bruker preview-funksjonalitet i Java 25 (structured concurrency) og må kjøres med `--enable-preview`.

---

## Henvendelser

Spørsmål knyttet til koden eller repositoryet kan stilles som issues her på GitHub

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team_dokumentløsninger.