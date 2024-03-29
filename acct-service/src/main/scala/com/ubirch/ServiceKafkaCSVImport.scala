package com.ubirch

import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths }
import com.ubirch.kafka.producer.{ Configs, ProducerRunner }
import com.ubirch.models.AcctEvent
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonFormatsProvider }
import com.ubirch.util.URLsHelper

import com.github.tototoshi.csv
import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import java.io.File
import java.text.SimpleDateFormat
import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps

object ServiceKafkaCSVImport extends LazyLogging {

  val config = new ConfigProvider get ()
  implicit val formats = new JsonFormatsProvider get ()
  lazy val jsonConverter = new DefaultJsonConverterService()

  lazy val bootstrapServers: String = URLsHelper.passThruWithCheck(config.getString(AcctConsumerConfPaths.BOOTSTRAP_SERVERS))
  lazy val lingerMs: Int = config.getInt(AcctProducerConfPaths.LINGER_MS)
  lazy val topic: String = config.getString(AcctConsumerConfPaths.ACCT_EVT_TOPIC_PATH)

  def main(args: Array[String]): Unit = {

    val path = args.headOption.getOrElse(throw new IllegalArgumentException("No path provided"))

    val configs = Configs(bootstrapServers, lingerMs = lingerMs)
    val producer = ProducerRunner[String, String](configs, Some(new StringSerializer()), Some(new StringSerializer()))

    val file: File = new File(path)

    //2021-08-25 13:53:16.399+0000
    lazy val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    //sdf.setTimeZone(TimeZone.getTimeZone("UTC"))

    if (file != null) {

      val reader: csv.CSVReader = null

      var processed = 0
      try {

        //These column names were obtained when running the command below
        //COPY acct_system.acct_events to 'acct_events_2021_1229_with_headers.csv' with HEADER=true ;
        //owner_id, identity_id, day, id, category, created_at, description, occurred_at, token_value
        val reader: csv.CSVReader = CSVReader.open(file)
        val iterator = reader.iteratorWithHeaders
        while (iterator.hasNext) {
          val current = iterator.next()
          val category = {
            val cat = current.getOrElse("category", throw new Exception("error with category"))
            if (cat == "verification") "upp_verification" else cat
          }
          val acctEvent: AcctEvent = AcctEvent(
            id = current.get("id").map(UUID.fromString).getOrElse(throw new Exception("error with id")),
            ownerId = current.get("owner_id").map(UUID.fromString),
            identityId = current.get("identity_id").map(UUID.fromString).getOrElse(throw new Exception("error with identity_id")),
            category = category.toLowerCase,
            subCategory = current.get("sub_category").map(_.toLowerCase),
            externalId = current.get("external_id"),
            occurredAt = current.get("occurred_at").map(sdf.parse).getOrElse(throw new Exception("error with occurred_at"))
          )
          val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
          val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
          producer.getProducerOrCreate.send(new ProducerRecord[String, String](topic, acctEventAsString))
          processed = processed + 1
        }

      } catch {
        case e: Exception =>
          logger.error(s"Something happened processing file: ${file.getName}", e)
      } finally {
        logger.info("processed=" + processed)
        if (reader != null) reader.close()
        producer.close(10 seconds)
        System.exit(0)
      }

    }

  }

}
