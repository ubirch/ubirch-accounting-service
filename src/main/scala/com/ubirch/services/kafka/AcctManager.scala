package com.ubirch
package services.kafka

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths, GenericConfPaths }
import com.ubirch.kafka.consumer.WithConsumerShutdownHook
import com.ubirch.kafka.express.ExpressKafka
import com.ubirch.kafka.producer.WithProducerShutdownHook
import com.ubirch.services.lifeCycle.Lifecycle
import com.ubirch.util.ServiceMetrics
import io.prometheus.client.Counter
import javax.inject._
import org.apache.kafka.common.serialization._

import scala.concurrent.ExecutionContext

abstract class AcctManager(val config: Config, lifecycle: Lifecycle)
  extends ExpressKafka[String, Array[Byte], Unit]
  with WithConsumerShutdownHook
  with WithProducerShutdownHook
  with ServiceMetrics
  with LazyLogging {

  override val service: String = config.getString(GenericConfPaths.NAME)

  override val successCounter: Counter = Counter.build()
    .name("acct_mgr_success")
    .help("Represents the number acct event successes")
    .labelNames("service", "acct")
    .register()

  override val errorCounter: Counter = Counter.build()
    .name("acct_mgr_failures")
    .help("Represents the number of acct event failures")
    .labelNames("service", "acct")
    .register()

  override val keyDeserializer: Deserializer[String] = new StringDeserializer
  override val valueDeserializer: Deserializer[Array[Byte]] = new ByteArrayDeserializer
  override val consumerTopics: Set[String] = Set(config.getString(AcctConsumerConfPaths.IMPORT_TOPIC_PATH))
  override val keySerializer: Serializer[String] = new StringSerializer
  override val valueSerializer: Serializer[Array[Byte]] = new ByteArraySerializer
  override val consumerBootstrapServers: String = config.getString(AcctConsumerConfPaths.BOOTSTRAP_SERVERS)
  override val consumerGroupId: String = config.getString(AcctConsumerConfPaths.GROUP_ID_PATH)
  override val consumerMaxPollRecords: Int = config.getInt(AcctConsumerConfPaths.MAX_POLL_RECORDS)
  override val consumerGracefulTimeout: Int = config.getInt(AcctConsumerConfPaths.GRACEFUL_TIMEOUT_PATH)
  override val metricsSubNamespace: String = config.getString(AcctConsumerConfPaths.METRICS_SUB_NAMESPACE)
  override val consumerReconnectBackoffMsConfig: Long = config.getLong(AcctConsumerConfPaths.RECONNECT_BACKOFF_MS_CONFIG)
  override val consumerReconnectBackoffMaxMsConfig: Long = config.getLong(AcctConsumerConfPaths.RECONNECT_BACKOFF_MAX_MS_CONFIG)
  override val maxTimeAggregationSeconds: Long = 120
  override val producerBootstrapServers: String = config.getString(AcctProducerConfPaths.BOOTSTRAP_SERVERS)
  override val lingerMs: Int = config.getInt(AcctProducerConfPaths.LINGER_MS)

  lifecycle.addStopHooks(hookFunc(consumerGracefulTimeout, consumption), hookFunc(production))

}

@Singleton
class DefaultAcctManager @Inject() (
    config: Config,
    lifecycle: Lifecycle
)(implicit val ec: ExecutionContext) extends AcctManager(config, lifecycle) {

  override val process: Process = Process { crs =>

    crs.foreach { _ => }

  }

  override def prefix: String = "Ubirch"

}
