package com.ubirch.controllers

import com.ubirch.models.Return
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.{ Awaits, EmbeddedCassandra, ExecutionContextsTests, FakeTokenCreator, InjectorHelperImpl }

import com.github.nosan.embedded.cassandra.api.cql.CqlScript
import io.prometheus.client.CollectorRegistry
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraWordSpec

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test for the Key Controller
  */
class AcctEventsControllerSpec
  extends ScalatraWordSpec
  with EmbeddedCassandra
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  private lazy val Injector = new InjectorHelperImpl() {}
  private val jsonConverter = Injector.get[JsonConverterService]
  private val tokenCreator = Injector.get[FakeTokenCreator]

  "Acct Events Service" must {

    "get OK" in {

      val token = tokenCreator.user

      def uuid = "963995ed-ce12-4ea5-89dc-b181701d1d7b"

      get(s"/v1/$uuid", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

    }

    "get OK when admin" in {

      val token = tokenCreator.admin

      def uuid = "963995ed-ce12-4ea5-89dc-b181701d1d7b"

      get(s"/v1/$uuid", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

    }

    "get OK with result" in {

      List(
        CqlScript.ofString(
          (s"INSERT INTO acct_system.acct_events (" +
            "owner_id, " +
            "identity_id, " +
            "day, " +
            "id, " +
            "category, " +
            "created_at, " +
            "description, " +
            "occurred_at) " +
            "VALUES (" +
            "963995ed-ce12-4ea5-89dc-b181701d1d7b, " +
            "7549acd8-91e1-4230-833a-2f386e09b96f, " +
            "'2020-12-03', " +
            "15f0c427-5dfb-4e15-853c-0770dd400763, " +
            "'verification'," +
            "'2020-12-03 19:44:44.261'," +
            "'Lana Del Rey - Bogota Concert'," +
            "'2020-12-03 19:44:43.243');").stripMargin
        ),
        CqlScript.ofString(
          (s"INSERT INTO acct_system.acct_events (" +
            "owner_id, " +
            "identity_id, " +
            "day, " +
            "id, " +
            "category, " +
            "created_at, " +
            "description, " +
            "occurred_at) " +
            "VALUES (" +
            "963995ed-ce12-4ea5-89dc-b181701d1d7b, " +
            "7549acd8-91e1-4230-833a-2f386e09b96c, " +
            "'2020-12-03', " +
            s"${UUID.randomUUID().toString}, " +
            "'verification'," +
            "'2020-12-03 19:44:44.261'," +
            "'Lana Del Rey - Bogota Concert'," +
            "'2020-12-03 19:44:43.243');").stripMargin
        )
      ).foreach(x => x.forEachStatement { x => val _ = cassandra.connection.execute(x) })

      val token = Injector.get[FakeTokenCreator].user

      def uuid = "963995ed-ce12-4ea5-89dc-b181701d1d7b"
      def identity = "7549acd8-91e1-4230-833a-2f386e09b96f"

      get(s"/v1/$uuid?identity_id=$identity", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[{"id":"15f0c427-5dfb-4e15-853c-0770dd400763","ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b","identityId":"7549acd8-91e1-4230-833a-2f386e09b96f","category":"verification","description":"Lana Del Rey - Bogota Concert","day":"2020-12-03","occurredAt":"2020-12-03T18:44:43.243Z","createdAt":"2020-12-03T18:44:44.261Z"}]}""".stripMargin
        assert(body == expected)
      }

    }

    "get OK with result with time range" in {

      List(
        CqlScript.ofClasspath("test_data.cql")
      ).foreach(_.forEachStatement { x => val _ = cassandra.connection.execute(x) })

      Thread.sleep(500)

      val token = Injector.get[FakeTokenCreator].admin

      def owner = "d63ecc03-f5a7-4d43-91d0-a30d034d8da3"
      def identity = "03ebd518-8b09-45ec-a039-604fc8a9e687"
      def start = "2020-01-01"
      def end = "2021-03-01"
      var category = "verification"
      var mode = "count"

      get(s"/v1/$owner?mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[3893]}""".stripMargin
        assert(body == expected)
      }

      get(s"/v1/$owner?identity_id=$identity&mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[544]}""".stripMargin
        assert(body == expected)
      }

      get(s"/v1/$owner?identity_id=$identity&start=$start&end=$end&mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[60]}""".stripMargin
        assert(body == expected)
      }

      get(s"/v1/$owner?identity_id=$identity&start=$start&end=$end&cat=$category&mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[60]}""".stripMargin
        assert(body == expected)
      }

      category = "anchoring"

      get(s"/v1/$owner?identity_id=$identity&start=$start&end=$end&cat=$category&mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[0]}""".stripMargin
        assert(body == expected)
      }

      mode = "bucketed"

      get(s"/v1/$owner?identity_id=$identity&start=$start&end=$end&mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":{"2021-02-27":6,"2021-02-23":3,"2021-02-24":12,"2021-02-25":6,"2021-02-18":3,"2021-02-21":1,"2021-02-26":18,"2021-02-28":1,"2021-02-22":6,"2021-03-01":2,"2021-02-16":2}}""".stripMargin
        assert(body == expected)
      }

      mode = "other"

      get(s"/v1/$owner?identity_id=$identity&start=$start&end=$end&mode=$mode", headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        val expected = """{"version":"1.0.0","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid mode: wrong mode param -> other"}""".stripMargin
        assert(body == expected)
      }

    }

    "fail when not authorized" in {

      val token = tokenCreator.user

      def uuid = UUID.randomUUID()

      get(s"/v1/$uuid", headers = Map("authorization" -> token.prepare)) {
        status should equal(403)
        assert(body == """{"version":"1.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Forbidden"}""")
      }

    }

    "fail when wrong uuid: get" in {

      def uuid = "WRONG_UUID"

      get(s"/v1/$uuid") {
        status should equal(401)
        assert(body == """{"version":"1.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: get" in {

      def uuid = UUID.randomUUID()

      get(s"/v1/$uuid") {
        status should equal(401)
        assert(body == """{"version":"1.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
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
