SET MODE Oracle;
CREATE SCHEMA IF NOT EXISTS JOARK;

create sequence joark.t_dokument_fil_seq start with 1 increment by 1;

create table joark.t_k_dokument_fil_opplasting_tilstand
(
    k_opplasting_tilstand varchar2(128 char) not null primary key
);

insert into joark.t_k_dokument_fil_opplasting_tilstand (k_opplasting_tilstand) values ('LASTER_OPP');
insert into joark.t_k_dokument_fil_opplasting_tilstand (k_opplasting_tilstand) values ('LASTET_OPP');
insert into joark.t_k_dokument_fil_opplasting_tilstand (k_opplasting_tilstand) values ('ARKIVERES');
insert into joark.t_k_dokument_fil_opplasting_tilstand (k_opplasting_tilstand) values ('ARKIVERT');
insert into joark.t_k_dokument_fil_opplasting_tilstand (k_opplasting_tilstand) values ('FEILET');

create table joark.t_dokument_fil_opplasting
(
    dokument_fil_id               varchar2(128 char) not null primary key,
    ekstern_dokument_referanse_id varchar2(512 char) not null,
    media_type                    varchar2(128 char) not null,
    sha256_sjekksum               varbinary(32)      not null,
    tilstand                      varchar2(128 char) not null
        constraint fk_dok_fil_opplasting_tilstand references joark.t_k_dokument_fil_opplasting_tilstand (k_opplasting_tilstand),
    opprettet_kilde_navn          varchar2(512 char) not null,
    dato_opprettet                timestamp          not null,
    dato_klient_lease             timestamp          not null,
    versjon                       number             not null,
    endret_kilde_navn             varchar2(512 char),
    dato_endret                   timestamp,
    crc32c_sjekksum               varbinary(4),
    antall_bytes                  number,
    dato_lastet_opp               timestamp,
    dato_arkivert                 timestamp,
    forsok                        number default 0   not null,
    constraint uidx_dok_fil_opplasting_ekst_ref unique (ekstern_dokument_referanse_id)
);

create table joark.t_dokument_fil
(
    dokument_fil_id      number(11)                             not null
        constraint xpkdokument_fil primary key,
    fil_uuid             varchar2(36)                           not null
        constraint xak1dokument_fil unique,
    fil                  blob                                   not null,
    dato_opprettet       timestamp(6)                           not null,
    opprettet_av         varchar2(40)                           not null,
    dato_endret          timestamp(6),
    endret_av            varchar2(40),
    versjon              number(11)                             not null,
    opprettet_kilde_navn varchar2(40) default 'KONVERTERT_FELT' not null,
    endret_kilde_navn    varchar2(40)
);
