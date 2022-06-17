package com.ubirch.controllers

import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths }
import com.ubirch._
import com.ubirch.kafka.util.PortGiver
import com.ubirch.models.AcctEvent
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.services.kafka.AcctManager
import com.ubirch.util.DateUtil

import com.google.inject.binder.ScopedBindingBuilder
import com.typesafe.config.{ Config, ConfigValueFactory }
import io.prometheus.client.CollectorRegistry
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraWordSpec

import java.text.SimpleDateFormat
import java.util.{ Date, UUID }
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test for the Key Controller
  */
class AcctEventsControllerKafkaSpec
  extends ScalatraWordSpec
  with EmbeddedKafka
  with EmbeddedCassandra
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  def FakeInjector(bootstrapServers: String, acctEvtTopic: String) = new InjectorHelper(List(new Binder {
    override def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(new ConfigProvider {
      override def conf: Config = super.conf
        .withValue(AcctConsumerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
        .withValue(AcctProducerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
        .withValue(AcctConsumerConfPaths.ACCT_EVT_TOPIC_PATH, ConfigValueFactory.fromAnyRef(acctEvtTopic))
    })
  })) {}

  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = PortGiver.giveMeKafkaPort, zooKeeperPort = PortGiver.giveMeZookeeperPort)

  val acctEvtTopic = "ubirch-acct-evt-json"

  lazy val Injector = FakeInjector("localhost:" + kafkaConfig.kafkaPort, acctEvtTopic)

  lazy val jsonConverter = Injector.get[JsonConverterService]

  "Acct Events Service" must {

    "record event through http" in {

      val batch = 50

      def id = UUID.randomUUID()
      def externalId = Option.empty[String]
      val ownerId = UUID.randomUUID()
      val identityId = UUID.fromString("12539f76-c7e9-47d6-b37b-4b59380721ac")
      val date = new Date()

      val acctEvents = (1 to batch).map { _ =>
        AcctEvent(id, Option(ownerId), identityId, "verification", Some("entry-a"), externalId, date)
      }.toList

      val acctEventAsJValue = jsonConverter.toJValue[List[AcctEvent]](acctEvents).getOrElse(throw new Exception("Not able to parse to string"))
      val acctEventAsString = jsonConverter.toString(acctEventAsJValue)

      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"

      withRunningKafka {

        val acctManager = Injector.get[AcctManager]
        acctManager.consumption.startPolling()

        post(s"/v1/record", body = acctEventAsString, headers = Map("authorization" -> s"bearer $token")) {
          status should equal(202)
        }

        Thread.sleep(5000)

        lazy val sdf = new SimpleDateFormat("yyyy-MM")

        val formattedDate = sdf.format(date)

        get(s"/v1/${identityId.toString}?date=$formattedDate&cat=verification", headers = Map("authorization" -> s"bearer $token")) {
          status should equal(200)
          val expected = s"""{"version":"1.0.0","ok":true,"data":[{"year":2022,"month":${DateUtil.dateToLocalDate(date).getMonthValue},"count":50}]}""".stripMargin
          assert(body == expected)
        }

      }

    }

  }

  override protected def beforeEach(): Unit = {
    CollectorRegistry.defaultRegistry.clear()
    EmbeddedCassandra.truncateScript.forEachStatement { x => val _ = cassandra.connection.execute(x) }
  }

  protected override def afterAll(): Unit = {
    cassandra.stop()
    super.afterAll()
  }

  protected override def beforeAll(): Unit = {

    CollectorRegistry.defaultRegistry.clear()
    cassandra.startAndCreateDefaults()

    lazy val pool = Injector.get[PublicKeyPoolService]
    await(pool.init, 2 seconds)

    lazy val tokenController = Injector.get[AcctEventsController]

    addServlet(tokenController, "/*")

    super.beforeAll()
  }
}
