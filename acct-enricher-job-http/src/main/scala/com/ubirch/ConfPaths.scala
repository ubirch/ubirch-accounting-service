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

  object GenericConfPaths extends GenericConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object TokenVerificationPaths extends TokenVerificationPaths

}
