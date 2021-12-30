/* Cassandra migration for keyspace acct_system.
   Version 1 - 2020-11-05T15:35:01.985909+00:00

   Adding basic acct table */

drop table if exists acct_system.acct_events;

create table if not exists acct_system.acct_events (id UUID,
                                        owner_id UUID,
                                        identity_id UUID,
                                        category text,
                                        description text,
                                        day date,
                                        occurred_at timestamp,
                                        created_at timestamp,
                                        PRIMARY KEY ((owner_id), identity_id, day, id));

