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

  trait PostgresPaths {
    final val MIGRATE_ON_START = "system.migrateOnStart"
    final val SERVER_NAME = "system.database.dataSource.serverName"
    final val USER = "system.database.dataSource.user"
    final val PASSWORD = "system.database.dataSource.password"
    final val PORT = "system.database.dataSource.portNumber"
    final val DATABASE_NAME = "system.database.dataSource.databaseName"
  }

  trait CassandraClusterConfPaths {
    final val CONTACT_POINTS = "acctSystem.cassandra.cluster.contactPoints"
    final val CONSISTENCY_LEVEL = "acctSystem.cassandra.cluster.consistencyLevel"
    final val SERIAL_CONSISTENCY_LEVEL = "acctSystem.cassandra.cluster.serialConsistencyLevel"
    final val WITH_SSL = "acctSystem.cassandra.cluster.withSSL"
    final val TRUST_STORE = "acctSystem.cassandra.cluster.trustStore"
    final val TRUST_STORE_PASSWORD = "acctSystem.cassandra.cluster.trustStorePassword"
    final val USERNAME = "acctSystem.cassandra.cluster.username"
    final val PASSWORD = "acctSystem.cassandra.cluster.password"
    final val KEYSPACE = "acctSystem.cassandra.cluster.keyspace"
    final val PREPARED_STATEMENT_CACHE_SIZE = "acctSystem.cassandra.cluster.preparedStatementCacheSize"
  }

  trait ExecutionContextConfPaths {
    final val THREAD_POOL_SIZE = "system.executionContext.threadPoolSize"
  }

  trait PrometheusConfPaths {
    final val PORT = "system.metrics.prometheus.port"
  }

  trait TokenVerificationPaths {
    final val CONFIG_URL = "system.tokenVerification.configURL"
    final val KID = "system.tokenVerification.kid"
  }

  trait ThingAPIConfPaths {
    final val URL = "system.thingAPI.url"
    final val REALM_NAME = "system.thingAPI.realmName"
  }

  object PostgresPaths extends PostgresPaths
  object GenericConfPaths extends GenericConfPaths
  object CassandraClusterConfPaths extends CassandraClusterConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object TokenVerificationPaths extends TokenVerificationPaths
  object ThingAPIConfPaths extends ThingAPIConfPaths

}
