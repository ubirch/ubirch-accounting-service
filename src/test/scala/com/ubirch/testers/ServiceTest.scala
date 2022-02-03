package com.ubirch.testers

import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths }
import com.ubirch.kafka.producer.{ Configs, ProducerRunner }
import com.ubirch.models.AcctEvent
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.util.URLsHelper
import com.ubirch.{ Binder, Boot }

import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import java.util.concurrent.CountDownLatch
import java.util.{ Calendar, Date, UUID }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

object ServiceTest extends Boot(Binder.modules) {

  val config: Config = get[Config]
  val jsonConverter = get[JsonConverterService]
  implicit val ex = get[ExecutionContext]

  val bootstrapServers: String = URLsHelper.passThruWithCheck(config.getString(AcctConsumerConfPaths.BOOTSTRAP_SERVERS))
  val lingerMs: Int = config.getInt(AcctProducerConfPaths.LINGER_MS)
  val topic: String = config.getString(AcctConsumerConfPaths.ACCT_EVT_TOPIC_PATH)

  def main(args: Array[String]): Unit = {

    val configs = Configs(bootstrapServers, lingerMs = lingerMs)
    val producer = ProducerRunner[String, String](configs, Some(new StringSerializer()), Some(new StringSerializer()))

    try {

      logger.info("Sending to " + topic)

      val batch = 5

      def id = UUID.randomUUID()
      val ownerId = UUID.fromString("15124619-f7b6-44ca-bce5-8ee1b2b159f3")
      val identityId = UUID.fromString("12539f76-c7e9-47d6-b37b-4b59380721ac")

      println("id= " + id)
      println("ownerId= " + ownerId)
      println("identityId= " + identityId)

      val latch = new CountDownLatch(1)

      Future.sequence {
        (1 to batch).map { _ =>
          val acctEvent: AcctEvent = AcctEvent(
            id,
            ownerId,
            identityId,
            "verification",
            "entry-b",
            new Date()
          )
          val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
          val acctEventAsString = jsonConverter.toString(acctEventAsJValue)

          producer.send(new ProducerRecord[String, String](topic, acctEventAsString))

        }.toList ++ (1 to batch).map { _ =>

          val c = Calendar.getInstance()
          c.setTime(new Date)
          c.add(Calendar.DATE, 1)

          val acctEvent: AcctEvent = AcctEvent(
            id,
            ownerId,
            identityId,
            "verification",
            "entry-a",
            c.getTime
          )
          val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
          val acctEventAsString = jsonConverter.toString(acctEventAsJValue)

          producer.send(new ProducerRecord[String, String](topic, acctEventAsString))

        }.toList

      }.onComplete {
        case Failure(exception) =>
          exception.printStackTrace()
          latch.countDown()
        case Success(_) =>
          println("done")
          latch.countDown()
      }

      latch.await()

    } finally {
      producer.close(5 seconds)
      System.exit(0)

    }

  }

}
