package com.ubirch

import com.github.nosan.embedded.cassandra.commons.ClassPathResource
import com.github.nosan.embedded.cassandra.cql.{ CqlScript, ResourceCqlScript, StringCqlScript }

object EmbeddedCassandra {

  def truncateScript: StringCqlScript = {
    new StringCqlScript("truncate acct_system.acct_events;")
  }
  def creationScripts: Seq[CqlScript] = List(
    new StringCqlScript("drop keyspace IF EXISTS acct_system;"),
    new StringCqlScript("CREATE KEYSPACE acct_system WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};"),
    new StringCqlScript("USE acct_system;"),
    new ResourceCqlScript(new ClassPathResource("db/migrations/v1_Adding_acct_events_table.cql")),
    new ResourceCqlScript(new ClassPathResource("db/migrations/v2_Adding_owner_table.cql"))
  )
}
