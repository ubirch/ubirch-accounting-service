package com.ubirch

/**
  * Object that contains configuration keys
  */
object ConfPaths {

  trait GenericConfPaths {
    final val NAME = "system.name"
  }

  trait HttpServerConfPaths {
    final val PORT = "system.server.port"
    final val SWAGGER_PATH = "system.server.swaggerPath"
  }

  trait PostgresConfPaths {
    final val MIGRATE_ON_START = "system.migrateOnStart"
    final val SERVER_NAME = "system.database.dataSource.serverName"
    final val USER = "system.database.dataSource.user"
    final val PASSWORD = "system.database.dataSource.password"
    final val PORT = "system.database.dataSource.portNumber"
    final val DATABASE_NAME = "system.database.dataSource.databaseName"
  }

  trait CassandraClusterConfPaths {
    final val CONTACT_POINTS = "system.cassandra.cluster.contactPoints"
    final val CONSISTENCY_LEVEL = "system.cassandra.cluster.consistencyLevel"
    final val SERIAL_CONSISTENCY_LEVEL = "system.cassandra.cluster.serialConsistencyLevel"
    final val WITH_SSL = "system.cassandra.cluster.withSSL"
    final val TRUST_STORE = "system.cassandra.cluster.trustStore"
    final val TRUST_STORE_PASSWORD = "system.cassandra.cluster.trustStorePassword"
    final val USERNAME = "system.cassandra.cluster.username"
    final val PASSWORD = "system.cassandra.cluster.password"
    final val KEYSPACE = "system.cassandra.cluster.keyspace"
    final val PREPARED_STATEMENT_CACHE_SIZE = "system.cassandra.cluster.preparedStatementCacheSize"
  }

  trait ExecutionContextConfPaths {
    final val THREAD_POOL_SIZE = "system.executionContext.threadPoolSize"
  }

  trait PrometheusConfPaths {
    final val PORT = "system.metrics.prometheus.port"
  }

  trait ThingAPIConfPaths {
    final val URL = "system.thingAPI.url"
    final val REALM_NAME = "system.thingAPI.realmName"
  }

  trait JobConfPaths {
    final val UBIRCH_TOKEN = "system.ubirchToken"
  }

  object PostgresConfPaths extends PostgresConfPaths
  object GenericConfPaths extends GenericConfPaths
  object CassandraClusterConfPaths extends CassandraClusterConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object ThingAPIConfPaths extends ThingAPIConfPaths
  object JobConfPaths extends JobConfPaths

}
