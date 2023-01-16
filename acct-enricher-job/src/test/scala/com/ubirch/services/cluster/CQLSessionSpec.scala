package com.ubirch.services.cluster

import com.ubirch.{ Binder, EmbeddedCassandra, InjectorHelper, InjectorHelperImpl, TestBase }
import com.github.nosan.embedded.cassandra.cql.StringCqlScript
import com.google.inject.Guice
import io.getquill.context.ExecutionInfo

/**
  * Test for the cassandra cluster
  */
class CQLSessionSpec extends TestBase with EmbeddedCassandra {
  override def getInjector: InjectorHelper = new InjectorHelperImpl() {}

  val cassandra = new CassandraTest

  lazy val serviceInjector = Guice.createInjector(new Binder())

  "Cluster and Cassandra Context" must {

    "be able to get proper instance and do query" in {

      val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

      val db = connectionService.context

      val t = db.executeQuery("SELECT * FROM acct_events")(ExecutionInfo.unknown, ()).headOptionL.runToFuture
      assert(await(t).nonEmpty)
    }

    "be able to get proper instance and do query without recreating it" in {

      val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

      val db = connectionService.context

      val t = db.executeQuery("SELECT * FROM acct_events")(ExecutionInfo.unknown, ()).headOptionL.runToFuture
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
      new StringCqlScript(
        """
          |drop table if exists acct_system.acct_events;
          |create table if not exists acct_system.acct_events
          |(
          |    id           UUID,
          |    identity_id  UUID,
          |    category     text,
          |    sub_category text,
          |    year         int,
          |    month        int,
          |    day          int,
          |    hour         int,
          |    occurred_at  timestamp,
          |    external_id   text,
          |    PRIMARY KEY ((identity_id, category, year, month, day, hour), sub_category, id)
          |);
          |""".stripMargin
      ),
      new StringCqlScript(
        ("INSERT INTO acct_system.acct_events (" +
          "id, " +
          "identity_id, " +
          "category, " +
          "sub_category, " +
          "year, " +
          "month, " +
          "day, " +
          "hour, " +
          "occurred_at) " +
          "VALUES (" +
          "963995ed-ce12-4ea5-89dc-b181701d1d7b, " +
          "15f0c427-5dfb-4e15-853c-0770dd400763, " +
          "'verification', " +
          "'default'," +
          "2020, " +
          "12, " +
          "03, " +
          "19," +
          "'2020-12-03 19:44:43.243'" +
          ");").stripMargin
      )
    )).foreach(x => x.forEachStatement { x => val _ = cassandra.session.execute(x) })
  }
}
