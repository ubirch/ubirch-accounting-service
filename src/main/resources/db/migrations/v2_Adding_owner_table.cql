/* Cassandra migration for keyspace acct_system.
   Version 2 - 2022-02-07T10:44:42.547746+00:00

   adding_owner_table */

drop table if exists acct_system.acct_event_owners;

create table if not exists acct_system.acct_event_owners
(
    owner_id    UUID,
    identity_id UUID,
    created_at   timestamp,
    PRIMARY KEY (owner_id, identity_id)
);
