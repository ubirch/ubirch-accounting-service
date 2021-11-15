package com.ubirch.services.cluster

import com.ubirch.{ Binder, EmbeddedCassandra, TestBase }

import com.github.nosan.embedded.cassandra.api.cql.CqlScript
import com.google.inject.Guice

/**
  * Test for the cassandra cluster
  */
class ClusterSpec extends TestBase with EmbeddedCassandra {

  val cassandra = new CassandraTest

  lazy val serviceInjector = Guice.createInjector(new Binder())

  "Cluster and Cassandra Context" must {

    "be able to get proper instance and do query" in {

      val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

      val db = connectionService.context

      val t = db.executeQuery("SELECT * FROM acct_events").headOptionL.runToFuture
      assert(await(t).nonEmpty)
    }

    "be able to get proper instance and do query without recreating it" in {

      val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

      val db = connectionService.context

      val t = db.executeQuery("SELECT * FROM acct_events").headOptionL.runToFuture
      assert(await(t).nonEmpty)
    }

  }

  override protected def afterAll(): Unit = {

    val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

    val db = connectionService.context

    db.close()

    cassandra.stop()
  }

  override protected def beforeAll(): Unit = {
    cassandra.start()

    (EmbeddedCassandra.creationScripts ++ List(
      CqlScript.ofString(
        "INSERT INTO acct_system.acct_events (owner_id, identity_id, day, id, category, created_at, description, occurred_at, token_value) VALUES (963995ed-ce12-4ea5-89dc-b181701d1d7b, 7549acd8-91e1-4230-833a-2f386e09b96f, '2020-12-03 11:00:00.000', 15f0c427-5dfb-4e15-853c-0770dd400763, 'verification', '2020-12-03 19:44:44.261', 'Lana Del Rey - Bogota Concert', '2020-12-03 19:44:43.243', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTg0MTI0MjYsImlhdCI6MTYwNzAyMjAyNiwianRpIjoiNDhkOGE5ZjUtMjY4Mi00YzU5LThjMDctM2NiN2VjMzVkMmE1IiwicHVycG9zZSI6IkxhbmEgRGVsIFJleSAtIEJvZ290YSBDb25jZXJ0IiwidGFyZ2V0X2lkZW50aXRpZXMiOiIqIiwicm9sZSI6InZlcmlmaWVyIn0.asqbiVNGesd6-S-tE0M7uJs6Kw2xEVMASo9M6XpSrJhXdj4qrnW-9EcgdvV2FGowrTeEFqS0zrt7LxphkQh-Wg');".stripMargin
      )
    )).foreach(x => x.forEachStatement { x => val _ = cassandra.connection.execute(x) })
  }
}
