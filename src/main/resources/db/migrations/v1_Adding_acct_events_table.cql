/* Cassandra migration for keyspace acct_system.
   Version 1 - 2020-11-05T15:35:01.985909+00:00

   Adding basic acct table */

drop table if exists acct_system.acct_events;

create table if not exists acct_system.acct_events
(
    id           UUID,
    identity_id  UUID,
    category     text,
    sub_category text,
    year         int,
    month        int,
    day          int,
    hour         int,
    occurred_at  timestamp,
    PRIMARY KEY ((identity_id, category, year, month, day, hour), sub_category, id)
);

