package com.ubirch
package services.cluster

import com.ubirch.ConfPaths.CassandraClusterConfPaths
import com.ubirch.services.lifeCycle.Lifecycle

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import io.getquill.context.cassandra.encoding.{ Decoders, Encoders }
import io.getquill.{ CassandraStreamContext, NamingStrategy, SnakeCase }

import javax.inject._
import scala.concurrent.Future

trait ConnectionServiceBase[N <: NamingStrategy] {
  val context: CassandraStreamContext[N] with Encoders with Decoders
  val preparedStatementCacheSize: Long
}

/**
  * Component that represents a Connection Service whose Naming Strategy
  * is ShakeCase.
  */

trait ConnectionService extends ConnectionServiceBase[SnakeCase]

/**
  * Default Implementation of the Connection Service Component.
  * It add shutdown hooks.
  * @param clusterService Cluster Service Component.
  * @param config Configuration injected component.
  * @param lifecycle Lifecycle injected component that allows for shutdown hooks.
  */

@Singleton
class DefaultConnectionService @Inject() (clusterService: CQLSessionService, config: Config, lifecycle: Lifecycle)
  extends ConnectionService with CassandraClusterConfPaths with LazyLogging {

  val preparedStatementCacheSize: Long = config.getLong(PREPARED_STATEMENT_CACHE_SIZE)

  private def createContext() = new CassandraStreamContext[SnakeCase](
    SnakeCase,
    clusterService.cqlSession,
    preparedStatementCacheSize
  ) with Encoders with Decoders

  override val context = createContext()

  lifecycle.addStopHook { () =>
    logger.info("Shutting down Connection Service")
    Future.successful(context.close())
  }

}
