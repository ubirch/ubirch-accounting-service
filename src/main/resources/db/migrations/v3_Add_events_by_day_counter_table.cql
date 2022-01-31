/* Cassandra migration for keyspace acct_system.
   Version 3 - 2021-12-28T15:36:56.000190+00:00

   acct_events_counts_by_day */

drop table if exists acct_system.acct_events_counts_by_day;

create table if not exists acct_system.acct_events_counts_by_day (
                                                    identity_id UUID,
                                                    category text,
                                                    day date,
                                                    count_events counter,
                                                    PRIMARY KEY (identity_id, category, day));
