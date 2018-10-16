# --- !Ups

create table groups (
  id                            bigint auto_increment not null,
  name                          varchar(255),
  ownerid                       bigint,
  is_admin_group                boolean default false not null,
  constraint uq_groups_name unique (name),
  constraint pk_groups primary key (id)
);

create table users (
  id                            bigint auto_increment not null,
  user_name                     varchar(255),
  email                         varchar(255),
  password                      varchar(255),
  password_reset_required       boolean default false not null,
  quota_limit                   integer not null,
  constraint uq_users_user_name unique (user_name),
  constraint pk_users primary key (id)
);

create table groupmembers (
  user_id                       bigint not null,
  group_id                      bigint not null,
  constraint pk_groupmembers primary key (user_id,group_id)
);

alter table groups add constraint fk_groups_ownerid foreign key (ownerid) references users (id) on delete restrict on update restrict;

create index ix_groupmembers_users on groupmembers (user_id);
alter table groupmembers add constraint fk_groupmembers_users foreign key (user_id) references users (id) on delete restrict on update restrict;

create index ix_groupmembers_groups on groupmembers (group_id);
alter table groupmembers add constraint fk_groupmembers_groups foreign key (group_id) references groups (id) on delete restrict on update restrict;

insert into users (id, user_name, email, password, password_reset_required, quota_limit)
values (0, 'admin', 'admin@admin.com', 'admin', true, 10);
insert into users (id, user_name, email, password, password_reset_required, quota_limit)
values (1, 'peter', 'peter@gmx.com', 'peter', true, 10);
insert into users (id, user_name, email, password, password_reset_required, quota_limit)
values (2, 'klaus', 'klaus@gmx.com', 'klaus', true, 10);
insert into users (id, user_name, email, password, password_reset_required, quota_limit)
values (3, 'hans', 'hans@gmx.com', 'hans', true, 10);

insert into groups (id, name, ownerid, is_admin_group)
values (0, 'All', 0, false);
insert into groups (id, name, ownerid, is_admin_group)
values (1, 'Administrators', 0, true);
insert into groups (id, name, ownerid, is_admin_group)
values (2, 'Peters Group', 1, false);

insert into groupmembers (user_id, group_id)
values (0, 0);
insert into groupmembers (user_id, group_id)
values (1, 0);
insert into groupmembers (user_id, group_id)
values (2, 0);
insert into groupmembers (user_id, group_id)
values (3, 0);

insert into groupmembers (user_id, group_id)
values (0, 1);

insert into groupmembers (user_id, group_id)
values (0, 2);
insert into groupmembers (user_id, group_id)
values (1, 2);
insert into groupmembers (user_id, group_id)
values (2, 2);

# --- !Downs

alter table groups drop constraint if exists fk_groups_ownerid;

alter table groupmembers drop constraint if exists fk_groupmembers_users;
drop index if exists ix_groupmembers_users;

alter table groupmembers drop constraint if exists fk_groupmembers_groups;
drop index if exists ix_groupmembers_groups;

drop table if exists groups;

drop table if exists users;

drop table if exists groupmembers;

