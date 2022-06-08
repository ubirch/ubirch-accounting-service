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

  trait PostgresPaths {
    final val MIGRATE_ON_START = "system.migrateOnStart"
    final val SERVER_NAME = "system.database.dataSource.serverName"
    final val USER = "system.database.dataSource.user"
    final val PASSWORD = "system.database.dataSource.password"
    final val PORT = "system.database.dataSource.portNumber"
    final val DATABASE_NAME = "system.database.dataSource.databaseName"
  }

  trait ExecutionContextConfPaths {
    final val THREAD_POOL_SIZE = "acctSystem.executionContext.threadPoolSize"
  }

  trait PrometheusConfPaths {
    final val PORT = "acctSystem.metrics.prometheus.port"
  }

  trait TokenVerificationPaths {
    final val CONFIG_URL = "acctSystem.tokenVerification.configURL"
    final val KID = "acctSystem.tokenVerification.kid"
  }

  object PostgresPaths extends PostgresPaths
  object GenericConfPaths extends GenericConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object TokenVerificationPaths extends TokenVerificationPaths

}
