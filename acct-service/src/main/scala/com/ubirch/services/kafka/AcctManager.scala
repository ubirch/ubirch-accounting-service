package com.ubirch
package services.kafka

import com.datastax.oss.driver.api.core.AllNodesFailedException
import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths }
import com.ubirch.kafka.consumer.WithConsumerShutdownHook
import com.ubirch.kafka.express.ExpressKafka
import com.ubirch.kafka.producer.WithProducerShutdownHook
import com.ubirch.models.{ AcctEvent, AcctEventOwnerRow, AcctEventRow, AcctStoreDAO }
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.lifeCycle.Lifecycle
import com.ubirch.util.DateUtil
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.kafka.util.Exceptions.NeedForPauseException
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization._
import org.json4s.Formats

import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.util.concurrent.ExecutionException
import javax.inject._
import scala.concurrent.{ ExecutionContext, Promise }

abstract class AcctManager(val config: Config, lifecycle: Lifecycle)
  extends ExpressKafka[String, Array[Byte], Unit]
  with WithConsumerShutdownHook
  with WithProducerShutdownHook
  with LazyLogging {

  override val keyDeserializer: Deserializer[String] = new StringDeserializer
  override val valueDeserializer: Deserializer[Array[Byte]] = new ByteArrayDeserializer
  override val consumerTopics: Set[String] = Set(config.getString(AcctConsumerConfPaths.ACCT_EVT_TOPIC_PATH))
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

  def storeAcctEvents(consumerRecords: Vector[ConsumerRecord[String, Array[Byte]]])(implicit scheduler: Scheduler): Promise[Unit]

  def logic(consumerRecords: Vector[ConsumerRecord[String, Array[Byte]]])(implicit scheduler: Scheduler): CancelableFuture[Unit] = {
    Task.defer(Task.fromFuture(storeAcctEvents(consumerRecords).future)).runToFuture
  }

  lifecycle.addStopHooks(hookFunc(consumerGracefulTimeout, consumption), hookFunc(production))

}

@Singleton
class DefaultAcctManager @Inject() (
    acctStoreDAO: AcctStoreDAO,
    jsonConverterService: JsonConverterService,
    config: Config,
    lifecycle: Lifecycle
)(implicit val ec: ExecutionContext, val scheduler: Scheduler, formats: Formats) extends AcctManager(config, lifecycle) {

  override def storeAcctEvents(consumerRecords: Vector[ConsumerRecord[String, Array[Byte]]])(implicit scheduler: Scheduler): Promise[Unit] = {
    val p = Promise[Unit]()

    Observable.fromIterable(consumerRecords)
      .map(_.value())
      .mapEval { bytes =>

        Task(jsonConverterService.fromJsonInput[AcctEvent](new ByteArrayInputStream(bytes))(_.camelizeKeys))
          .map { acctEvent =>
            if (acctEvent.validate) acctEvent
            else {
              throw new Exception("Acct Event received is not valid. The validation process failed: " + acctEvent.toString)
            }
          }
          .doOnFinish { maybeError =>
            Task {
              maybeError.foreach { x =>
                logger.error("Error parsing: {}", x.getMessage)
              }
            }
          }
          .attempt

      }
      .collect {
        case Right(acctEvent) => acctEvent
      }
      .flatMap { acctEvent =>

        val day = DateUtil.dateToLocalTime(acctEvent.occurredAt, ZoneId.systemDefault())

        val eventsRow = AcctEventRow(
          id = acctEvent.id,
          identityId = acctEvent.identityId,
          category = acctEvent.category.toLowerCase,
          subCategory = acctEvent.subCategory.getOrElse("default").toLowerCase,
          year = day.getYear,
          month = day.getMonthValue,
          day = day.getDayOfMonth,
          hour = day.getHour,
          occurredAt = acctEvent.occurredAt.toInstant,
          externalId = acctEvent.externalId
        )
        val eventsOwnerRow = acctEvent.ownerId.map { o =>
          AcctEventOwnerRow(o, acctEvent.identityId, acctEvent.occurredAt.toInstant)
        }
        for {
          res <- acctStoreDAO
            .events
            .insert(eventsRow)
            .map(x => (acctEvent, eventsRow, x))

          _ <- eventsOwnerRow.map(eo => acctStoreDAO.owner.insert(eo)).getOrElse(Observable.unit)
        } yield res
      }
      .flatMap { case (_, row, _) =>
        logger.debug("acct_evt_inserted={}", row.toString)
        Observable.unit
      }
      .onErrorHandle {
        case e: ExecutionException =>
          e.getCause match {
            case e: AllNodesFailedException =>
              logger.error("Error connecting to host: " + e)
              p.failure(NeedForPauseException("Error connecting", e.getLocalizedMessage))
            case e: InvalidQueryException =>
              logger.error("Error storing data (invalid query): " + e)
              p.failure(StoringException("Invalid Query ", e.getMessage))
          }
        case e: Exception =>
          logger.error("Error storing data (other): " + e)
          p.failure(StoringException("Error storing data (other)", e.getMessage))
      }
      .doOnComplete(Task(p.success(())))
      .foreachL(_ => ())
      .runToFuture(consumption.scheduler)

    p
  }

  override val process: Process = Process.async(logic)

  override def prefix: String = "Ubirch"

}
