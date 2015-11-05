# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table cluster (
  id                        integer not null,
  cluster                   integer,
  result_set                integer,
  size                      integer,
  color_id                  integer,
  shape                     varchar(255),
  visible                   integer,
  label                     varchar(255),
  constraint pk_cluster primary key (id))
;

create table color (
  id                        integer not null,
  a                         integer,
  b                         integer,
  g                         integer,
  r                         integer,
  constraint pk_color primary key (id))
;

create table point (
  id                        integer not null,
  x                         float,
  y                         float,
  z                         float,
  cluster                   integer,
  result_set                integer,
  constraint pk_point primary key (id))
;

create table result_set (
  id                        integer not null,
  name                      varchar(255),
  description               varchar(255),
  date_creation             timestamp,
  uploader_id               integer,
  time_series_id            integer,
  time_series_seq_number    integer,
  file_name                 varchar(255),
  constraint pk_result_set primary key (id))
;

create table time_series (
  id                        integer not null,
  name                      varchar(255),
  description               varchar(255),
  date_creation             timestamp,
  uploader_id               integer,
  status                    varchar(255),
  constraint uq_time_series_name unique (name),
  constraint pk_time_series primary key (id))
;

create table user (
  id                        integer not null,
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

create sequence color_seq;

create sequence point_seq;

create sequence result_set_seq;

create sequence time_series_seq;

create sequence user_seq;

alter table cluster add constraint fk_cluster_color_1 foreign key (color_id) references color (id) on delete restrict on update restrict;
create index ix_cluster_color_1 on cluster (color_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists cluster;

drop table if exists color;

drop table if exists point;

drop table if exists result_set;

drop table if exists time_series;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists cluster_seq;

drop sequence if exists color_seq;

drop sequence if exists point_seq;

drop sequence if exists result_set_seq;

drop sequence if exists time_series_seq;

drop sequence if exists user_seq;

