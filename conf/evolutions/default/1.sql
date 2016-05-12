# --- !Ups

create table user (
  email                     varchar(255),
  password_hash             varchar(255),
  constraint uq_user_email unique (email))
;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

