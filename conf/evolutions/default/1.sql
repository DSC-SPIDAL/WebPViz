# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table cluster (
  id                        bigint not null,
  cluster                   integer,
  result_set                bigint,
  constraint pk_cluster primary key (id))
;

create table point (
  id                        bigint not null,
  x                         float,
  y                         float,
  z                         float,
  cluster                   bigint,
  result_set                bigint,
  constraint pk_point primary key (id))
;

create table result_set (
  id                        bigint not null,
  name                      varchar(255),
  description               varchar(255),
  date_creation             timestamp,
  uploader_id               bigint,
  time_series_id            bigint,
  time_series_seq_number    bigint,
  constraint pk_result_set primary key (id))
;

create table time_series (
  id                        bigint not null,
  name                      varchar(255),
  description               varchar(255),
  date_creation             timestamp,
  uploader_id               bigint,
  constraint uq_time_series_name unique (name),
  constraint pk_time_series primary key (id))
;

create table user (
  id                        bigint not null,
  email                     varchar(255),
  fullname                  varchar(255),
  password_hash             varchar(255),
  date_creation             timestamp,
  validated                 boolean,
  constraint uq_user_email unique (email),
  constraint uq_user_fullname unique (fullname),
  constraint pk_user primary key (id))
;

create sequence cluster_seq;

create sequence point_seq;

create sequence result_set_seq;

create sequence time_series_seq;

create sequence user_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists cluster;

drop table if exists point;

drop table if exists result_set;

drop table if exists time_series;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists cluster_seq;

drop sequence if exists point_seq;

drop sequence if exists result_set_seq;

drop sequence if exists time_series_seq;

drop sequence if exists user_seq;

