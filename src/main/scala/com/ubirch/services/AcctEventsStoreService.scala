package com.ubirch.services

import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths }
import com.ubirch.kafka.producer.{ Configs, ProducerRunner }
import com.ubirch.models.AcctEvent
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.util.URLsHelper

import com.typesafe.config.Config
import monix.eval.Task
import monix.reactive.Observable
import org.apache.kafka.clients.producer.{ ProducerRecord, RecordMetadata }
import org.apache.kafka.common.serialization.StringSerializer

import javax.inject.{ Inject, Singleton }

trait AcctEventsStoreService {
  def store(acctEvents: List[AcctEvent]): Task[List[RecordMetadata]]
}

@Singleton
class DefaultAcctEventsStoreService @Inject() (config: Config, jsonConverter: JsonConverterService) extends AcctEventsStoreService {

  val bootstrapServers: String = URLsHelper.passThruWithCheck(config.getString(AcctConsumerConfPaths.BOOTSTRAP_SERVERS))
  val lingerMs: Int = config.getInt(AcctProducerConfPaths.LINGER_MS)
  val topic: String = config.getString(AcctConsumerConfPaths.ACCT_EVT_TOPIC_PATH)
  val configs = Configs(bootstrapServers, lingerMs = lingerMs)
  val producer = ProducerRunner[String, String](configs, Some(new StringSerializer()), Some(new StringSerializer()))

  private def publish(acctEvent: AcctEvent): Task[RecordMetadata] = Task.deferFuture {
    val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
    val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
    producer.send(new ProducerRecord[String, String](topic, acctEventAsString))
  }

  override def store(acctEvents: List[AcctEvent]): Task[List[RecordMetadata]] = {
    Observable
      .fromIterable(acctEvents)
      .mapEval(publish)
      .toListL
  }

}
