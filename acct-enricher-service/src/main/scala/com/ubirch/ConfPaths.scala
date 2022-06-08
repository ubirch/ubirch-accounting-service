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

  trait PrometheusConfPaths {
    final val PORT = "acctSystem.metrics.prometheus.port"
  }

  trait TokenVerificationPaths {
    final val CONFIG_URL = "acctSystem.tokenVerification.configURL"
    final val KID = "acctSystem.tokenVerification.kid"
  }

  object GenericConfPaths extends GenericConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object TokenVerificationPaths extends TokenVerificationPaths

}
