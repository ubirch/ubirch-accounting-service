package com.ubirch

/**
  * Object that contains configuration keys
  */
object ConfPaths {

  trait GenericConfPaths {
    final val NAME = "acctSystem.name"
  }

  trait HttpServerConfPaths {
    final val PORT = "acctSystem.server.port"
    final val SWAGGER_PATH = "acctSystem.server.swaggerPath"
  }

  trait ExecutionContextConfPaths {
    final val THREAD_POOL_SIZE = "acctSystem.executionContext.threadPoolSize"
  }

  trait CassandraClusterConfPaths {
    final val CONTACT_POINTS = "acctSystem.cassandra.cluster.contactPoints"
    final val CONSISTENCY_LEVEL = "acctSystem.cassandra.cluster.consistencyLevel"
    final val SERIAL_CONSISTENCY_LEVEL = "acctSystem.cassandra.cluster.serialConsistencyLevel"
    final val WITH_SSL = "acctSystem.cassandra.cluster.withSSL"
    final val USERNAME = "acctSystem.cassandra.cluster.username"
    final val PASSWORD = "acctSystem.cassandra.cluster.password"
    final val KEYSPACE = "acctSystem.cassandra.cluster.keyspace"
    final val PREPARED_STATEMENT_CACHE_SIZE = "acctSystem.cassandra.cluster.preparedStatementCacheSize"
  }

  trait PrometheusConfPaths {
    final val PORT = "acctSystem.metrics.prometheus.port"
  }

  object GenericConfPaths extends GenericConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths

}
