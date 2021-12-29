package com.ubirch.services.kafka

import com.ubirch.ConfPaths.{ AcctConsumerConfPaths, AcctProducerConfPaths }
import com.ubirch._
import com.ubirch.kafka.util.PortGiver
import com.ubirch.models.{ AcctEvent, AcctEventCountDAO, AcctEventDAO }
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.formats.JsonConverterService

import com.google.inject.binder.ScopedBindingBuilder
import com.typesafe.config.{ Config, ConfigValueFactory }
import io.prometheus.client.CollectorRegistry
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }

import java.util.{ Date, UUID }
import scala.concurrent.duration._
import scala.language.postfixOps

class AcctManagerSpec extends TestBase with EmbeddedCassandra with EmbeddedKafka {

  val cassandra = new CassandraTest

  def FakeInjector(bootstrapServers: String, acctEvtTopic: String) = new InjectorHelper(List(new Binder {
    override def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(new ConfigProvider {
      override def conf: Config = super.conf
        .withValue(AcctConsumerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
        .withValue(AcctProducerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
        .withValue(AcctConsumerConfPaths.ACCT_EVT_TOPIC_PATH, ConfigValueFactory.fromAnyRef(acctEvtTopic))
    })
  })) {}

  "read and process acct events with success and errors" in {

    implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = PortGiver.giveMeKafkaPort, zooKeeperPort = PortGiver.giveMeZookeeperPort)

    val acctEvtTopic = "ubirch-acct-evt-json"

    val Injector = FakeInjector("localhost:" + kafkaConfig.kafkaPort, acctEvtTopic)

    val jsonConverter = Injector.get[JsonConverterService]
    val acctEventDAO = Injector.get[AcctEventDAO]

    val batch = 50

    def id = UUID.randomUUID()
    def ownerId = UUID.randomUUID()
    def identityId = UUID.randomUUID()

    val validAcctEvents = (1 to batch).map { _ =>
      val acctEvent: AcctEvent = AcctEvent(id, ownerId, Some(identityId), "verification", Some("Lana de rey concert"), Some("this is a token"), new Date())
      val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
      val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
      (acctEvent, acctEventAsString)
    }

    val invalidAcctEvents = (1 to batch).map { _ =>
      val acctEvent: AcctEvent = AcctEvent(id, ownerId, None, "verification", None, None, new Date())
      val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
      val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
      (acctEvent, acctEventAsString)
    }

    val totalAcctEvents = validAcctEvents ++ invalidAcctEvents

    withRunningKafka {

      totalAcctEvents.foreach { case (_, id) =>
        publishStringMessageToKafka(acctEvtTopic, id)
      }

      val acctManager = Injector.get[AcctManager]
      acctManager.consumption.startPolling()

      Thread.sleep(7000)

      val presentAcctEvents = await(acctEventDAO.selectAll, 5 seconds)

      assert(presentAcctEvents.nonEmpty)
      assert(presentAcctEvents.size == validAcctEvents.size)

    }

  }

  "read and process acct events with success and errors same sources" in {

    implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = PortGiver.giveMeKafkaPort, zooKeeperPort = PortGiver.giveMeZookeeperPort)

    val acctEvtTopic = "ubirch-acct-evt-json"

    val Injector = FakeInjector("localhost:" + kafkaConfig.kafkaPort, acctEvtTopic)

    val jsonConverter = Injector.get[JsonConverterService]
    val acctEventDAO = Injector.get[AcctEventDAO]

    val batch = 50

    def id = UUID.randomUUID()
    val ownerId = UUID.randomUUID()
    val identityId = UUID.randomUUID()

    val validAcctEvents = (1 to batch).map { _ =>
      val acctEvent: AcctEvent = AcctEvent(id, ownerId, Some(identityId), "verification", Some("Lana de rey concert"), Some("this is a token"), new Date())
      val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
      val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
      (acctEvent, acctEventAsString)
    }

    val invalidAcctEvents = (1 to batch).map { _ =>
      val acctEvent: AcctEvent = AcctEvent(id, ownerId, None, "verification", None, None, new Date())
      val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
      val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
      (acctEvent, acctEventAsString)
    }

    val totalAcctEvents = validAcctEvents ++ invalidAcctEvents

    withRunningKafka {

      totalAcctEvents.foreach { case (_, id) =>
        publishStringMessageToKafka(acctEvtTopic, id)
      }

      val acctManager = Injector.get[AcctManager]
      acctManager.consumption.startPolling()

      Thread.sleep(7000)

      val presentAcctEvents = await(acctEventDAO.selectAll, 5 seconds)

      assert(presentAcctEvents.nonEmpty)
      assert(presentAcctEvents.size == validAcctEvents.size)

    }

  }

  "read and process acct events with success with counters" in {

    implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = PortGiver.giveMeKafkaPort, zooKeeperPort = PortGiver.giveMeZookeeperPort)

    val acctEvtTopic = "ubirch-acct-evt-json"

    val Injector = FakeInjector("localhost:" + kafkaConfig.kafkaPort, acctEvtTopic)

    val jsonConverter = Injector.get[JsonConverterService]
    val acctEventDAO = Injector.get[AcctEventDAO]
    val acctEventCountDAO = Injector.get[AcctEventCountDAO]

    val batch = 50

    def id = UUID.randomUUID()
    val ownerId = UUID.randomUUID()
    val identityId = UUID.randomUUID()
    val category = "verification"

    val validAcctEvents = (1 to batch).map { _ =>
      val acctEvent: AcctEvent = AcctEvent(id, ownerId, Some(identityId), category, Some("Lana de rey concert"), Some("this is a token"), new Date())
      val acctEventAsJValue = jsonConverter.toJValue[AcctEvent](acctEvent).getOrElse(throw new Exception("Not able to parse to string"))
      val acctEventAsString = jsonConverter.toString(acctEventAsJValue)
      (acctEvent, acctEventAsString)
    }

    val totalAcctEvents = validAcctEvents

    withRunningKafka {

      totalAcctEvents.foreach { case (_, id) =>
        publishStringMessageToKafka(acctEvtTopic, id)
      }

      val acctManager = Injector.get[AcctManager]
      acctManager.consumption.startPolling()

      Thread.sleep(7000)

      val presentAcctEvents = await(acctEventDAO.selectAll, 5 seconds)

      assert(presentAcctEvents.nonEmpty)
      assert(presentAcctEvents.size == validAcctEvents.size)

      val presentAcctEventsCounts = await(acctEventCountDAO.byIdentityIdAndCategory(identityId, category), 5 seconds)

      assert(presentAcctEventsCounts.nonEmpty)
      assert(presentAcctEventsCounts.size == 1)
      assert(presentAcctEventsCounts.headOption.getOrElse(throw new Exception("empty counts")).countEvents == 50)

    }

  }

  override protected def beforeEach(): Unit = {
    CollectorRegistry.defaultRegistry.clear()
    EmbeddedCassandra.truncateScript.forEachStatement { x => val _ = cassandra.connection.execute(x) }
  }

  protected override def afterAll(): Unit = {
    cassandra.stop()
  }

  protected override def beforeAll(): Unit = {
    cassandra.startAndCreateDefaults()
  }

}
