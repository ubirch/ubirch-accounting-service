/* Cassandra migration for keyspace acct_system.
   Version 2 - 2021-11-16T15:35:01.985909+00:00

   Adding acct table by cat mv */

create materialized view acct_system.acct_events_by_cat
as
select category,
       owner_id,
       identity_id,
       id,
       category,
       description,
       day,
       occurred_at,
       created_at
from acct_system.acct_events
where owner_id is not null
  and identity_id is not null
  and id is not null
  and category is not null
  and day is not null
primary key ((owner_id, category), identity_id, day, id);

