SET MODE Oracle;
CREATE SCHEMA IF NOT EXISTS JOARK;

-- Opprett tabell siden dette ikke er en egen JPA entitet
create table joark.t_saksrelasjon
(
    sak_id         NUMBER        not null,
    feilregistrert CHAR(1),
    journalpost_id NUMBER(11, 0) not null
);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (123, '0', 123456);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (123, '0', 123457);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (123, '0', 123458);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (123, '0', 123459);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (234, '0', 234567);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (345, '0', 345678);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (456, '1', 456789);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (567, '0', 5678910);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (678, '0', 67891011);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (777, '0', 7771);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (777, '0', 7772);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (777, '0', 7773);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (777, '0', 7774);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (7898, '0', 123456);

insert into joark.t_saksrelasjon(sak_id, feilregistrert, journalpost_id)
VALUES (7899, '0', 123456);

create table joark.t_journalpost
(
    journalpost_id NUMBER(11, 0) not null,
    k_journal_s    VARCHAR2(20)  not null,
    journalf_enhet VARCHAR2(20),
    dato_journal   TIMESTAMP(6),
    dato_opprettet TIMESTAMP(6)
);

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('123456', 'FL', '1234', '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('123457', 'E', '1234', '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('123458', 'E', null, '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('123459', 'E', '1234', '2025-01-01T13:30', null);

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('234567', 'FS', '5678', '2025-02-13T14:45', '2025-02-13T15:00');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('345678', 'M', '5678', '2025-02-13T14:45', '2025-02-13T15:00');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('456789', 'FS', '5678', '2025-02-13T14:45', '2025-02-13T15:00');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('5678910', 'FS', '', '2025-02-13T14:45', '2025-02-13T15:00');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('67891011', 'FL', '1111', '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('7771', 'A', '1111', '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('7772', 'U', '1111', '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('7773', 'UB', '1111', '2025-01-01T13:30', '2025-01-02T13:30');

insert into joark.t_journalpost(journalpost_id, k_journal_s, journalf_enhet, dato_opprettet, dato_journal)
VALUES ('7774', 'FS', '1111', '2025-01-01T13:30', '2025-01-02T13:30');

create table joark.sak
(
    ID                   NUMBER(10, 0) not null,
    K_SAK_STATUS         VARCHAR2(40),
    K_AVLEVERING_STATUS  VARCHAR2(128),
    K_KASSASJON_STATUS   VARCHAR2(128),
    ENDRET_AV            VARCHAR2(40),
    ENDRET_KILDE_NAVN    VARCHAR2(40),
    DATO_ENDRET          TIMESTAMP(6),
    DATO_AVSLUTTET       TIMESTAMP(6),
    AVSLUTTET_AV         VARCHAR2(40),
    AVSLUTTET_KILDE_NAVN VARCHAR2(40),
    DATO_SAK_OPPRETTET   TIMESTAMP(6),
    ADMINISTRATIV_ENHET  VARCHAR2(40),
    SAK_ANSVARLIG        VARCHAR2(40),
    FAGSAKNR             VARCHAR2(40)
);

insert into joark.sak(ID, K_SAK_STATUS)
VALUES (123, 'AAPEN');

insert into joark.sak(ID)
VALUES (234);

insert into joark.sak(ID)
VALUES (345);

insert into joark.sak(ID)
VALUES (456);

insert into joark.sak(ID)
VALUES (568);

insert into joark.sak(ID)
VALUES (678);

insert into joark.sak(ID)
VALUES (777);

insert into joark.sak(ID, FAGSAKNR)
VALUES (7898, 'FAGSAK_123');

insert into joark.sak(ID, FAGSAKNR)
VALUES (7899, 'FAGSAK_234');

create table joark.t_administrativ_enhet
(
    ADMINISTRATIV_ENHET_ID NUMBER(19, 0) not null,
    TEMA                   VARCHAR2(128) not null,
    DATO_FOM               TIMESTAMP(6) not null,
    DATO_TOM               TIMESTAMP(6) not null,
    ENHET_NAVN             VARCHAR2(40) not null
);

insert into joark.t_administrativ_enhet(administrativ_enhet_id, tema, dato_fom, dato_tom, enhet_navn)
VALUES (1, 'AAP', '2009-11-19', '2099-01-01', 'Nav-kontor');

insert into joark.t_administrativ_enhet(administrativ_enhet_id, tema, dato_fom, dato_tom, enhet_navn)
VALUES (2, 'UFM', '2015-01-15', '2015-12-31', 'Nav Internasjonalt');

insert into joark.t_administrativ_enhet(administrativ_enhet_id, tema, dato_fom, dato_tom, enhet_navn)
VALUES (3, 'UFM', '2016-01-01', '2099-01-01', 'Nav Medlemskap og avgift');

create table joark.t_slettebestilling
(
    SLETTEBESTILLING_ID        number(19)    not null primary key,
    K_SLETTEBESTILLING_TYPE    varchar2(128) not null,
    K_SLETTEBESTILLING_STATUS  varchar2(128) not null,
    K_SLETTEBESTILLING_HJEMMEL varchar2(128) not null,
    K_SLETTEBESTILLING_ARSAK   varchar2(128) not null,
    BEGRUNNELSE                varchar2(512),
    SAK_ID                     number(19),
    DATO_UTFORES               date      not null,
    DATO_OPPRETTET             timestamp not null,
    OPPRETTET_AV               varchar2(512) not null,
    OPPRETTET_AV_NAVN          varchar2(512) not null,
    OPPRETTET_KILDE_NAVN       varchar2(512) not null
);

CREATE SEQUENCE JOARK.T_SLETTEBESTILLING_SEQ;
