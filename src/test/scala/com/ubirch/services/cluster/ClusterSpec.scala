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
        "INSERT INTO acct_system.acct_events (owner_id, identity_id, day, id, category, created_at, description, occurred_at) VALUES (963995ed-ce12-4ea5-89dc-b181701d1d7b, 7549acd8-91e1-4230-833a-2f386e09b96f, '2020-12-03', 15f0c427-5dfb-4e15-853c-0770dd400763, 'verification', '2020-12-03 19:44:44.261', 'Lana Del Rey - Bogota Concert', '2020-12-03 19:44:43.243');".stripMargin
      )
    )).foreach(x => x.forEachStatement { x => val _ = cassandra.connection.execute(x) })
  }
}
