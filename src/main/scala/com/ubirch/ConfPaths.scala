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

  trait AcctConsumerConfPaths {
    final val BOOTSTRAP_SERVERS = "acctSystem.kafkaConsumer.bootstrapServers"
    final val ACCT_EVT_TOPIC_PATH = "acctSystem.kafkaConsumer.acctEvtTopic"
    final val MAX_POLL_RECORDS = "acctSystem.kafkaConsumer.maxPollRecords"
    final val GROUP_ID_PATH = "acctSystem.kafkaConsumer.groupId"
    final val GRACEFUL_TIMEOUT_PATH = "acctSystem.kafkaConsumer.gracefulTimeout"
    final val METRICS_SUB_NAMESPACE = "acctSystem.kafkaConsumer.metricsSubNamespace"
    final val FETCH_MAX_BYTES_CONFIG = "acctSystem.kafkaConsumer.fetchMaxBytesConfig"
    final val MAX_PARTITION_FETCH_BYTES_CONFIG = "acctSystem.kafkaConsumer.maxPartitionFetchBytesConfig"
    final val RECONNECT_BACKOFF_MS_CONFIG = "acctSystem.kafkaConsumer.reconnectBackoffMsConfig"
    final val RECONNECT_BACKOFF_MAX_MS_CONFIG = "acctSystem.kafkaConsumer.reconnectBackoffMaxMsConfig"
  }

  trait AcctProducerConfPaths {
    final val LINGER_MS = "acctSystem.kafkaProducer.lingerMS"
    final val BOOTSTRAP_SERVERS = "acctSystem.kafkaProducer.bootstrapServers"
    final val ERROR_TOPIC_PATH = "acctSystem.kafkaProducer.errorTopic"
  }

  trait PrometheusConfPaths {
    final val PORT = "acctSystem.metrics.prometheus.port"
  }

  trait TokenVerificationPaths {
    final val CONFIG_URL = "acctSystem.tokenVerification.configURL"
    final val KID = "acctSystem.tokenVerification.kid"
  }

  object GenericConfPaths extends GenericConfPaths
  object AcctConsumerConfPaths extends AcctConsumerConfPaths
  object AcctProducerConfPaths extends AcctProducerConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object TokenVerificationPaths extends TokenVerificationPaths

}
