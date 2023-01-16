package com.ubirch

import com.datastax.oss.driver.api.core.CqlSession
import com.github.nosan.embedded.cassandra.{ Cassandra, CassandraBuilder }
import com.github.nosan.embedded.cassandra.cql.{ CqlScript, StringCqlScript }
import com.typesafe.scalalogging.LazyLogging

import java.net.InetSocketAddress
import scala.collection.JavaConverters._

/**
  * Tool for embedding cassandra
  */
trait EmbeddedCassandra {

  class CassandraTest extends LazyLogging {

    @volatile var cassandra: Cassandra = _
    @volatile var session: CqlSession = _

    def start(): Unit = {
      cassandra = new CassandraBuilder().addJvmOptions(List("-Xms500m", "-Xmx1000m").asJava).build()
      cassandra.start()
      val settings = cassandra.getSettings()
      session = CqlSession.builder()
        .addContactPoint(new InetSocketAddress(settings.getAddress(), settings.getPort()))
        .withLocalDatacenter("datacenter1")
        .build()
    }

    def stop(): Unit = {
      if (session != null) try session.close()
      catch {
        case ex: Throwable =>
          logger.error("CqlSession '" + session + "' is not closed", ex)
      }
      cassandra.stop()
      if (cassandra != null) cassandra.stop()
    }

    def startAndCreateDefaults(scripts: Seq[CqlScript] = EmbeddedCassandra.creationScripts): Unit = {
      start()
      scripts.foreach(x => x.forEachStatement { x => val _ = session.execute(x) })
    }

  }

}

object EmbeddedCassandra {

  def truncateScript: StringCqlScript = {
    new StringCqlScript("truncate acct_system.acct_events;")
  }

  def creationScripts: Seq[CqlScript] = List(
    new StringCqlScript("drop keyspace IF EXISTS acct_system;"),
    new StringCqlScript("CREATE KEYSPACE acct_system WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};"),
    new StringCqlScript("USE acct_system;")
  )
}
