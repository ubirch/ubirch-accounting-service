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
            "occurred_at, " +
            "token_value) " +
            "VALUES (" +
            "963995ed-ce12-4ea5-89dc-b181701d1d7b, " +
            "7549acd8-91e1-4230-833a-2f386e09b96f, " +
            "'2020-12-03 11:00:00.000', " +
            "15f0c427-5dfb-4e15-853c-0770dd400763, " +
            "'verification'," +
            "'2020-12-03 19:44:44.261'," +
            "'Lana Del Rey - Bogota Concert'," +
            "'2020-12-03 19:44:43.243'," +
            "'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTg0MTI0MjYsImlhdCI6MTYwNzAyMjAyNiwianRpIjoiNDhkOGE5ZjUtMjY4Mi00YzU5LThjMDctM2NiN2VjMzVkMmE1IiwicHVycG9zZSI6IkxhbmEgRGVsIFJleSAtIEJvZ290YSBDb25jZXJ0IiwidGFyZ2V0X2lkZW50aXRpZXMiOiIqIiwicm9sZSI6InZlcmlmaWVyIn0.asqbiVNGesd6-S-tE0M7uJs6Kw2xEVMASo9M6XpSrJhXdj4qrnW-9EcgdvV2FGowrTeEFqS0zrt7LxphkQh-Wg');").stripMargin
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
            "occurred_at, " +
            "token_value) " +
            "VALUES (" +
            "963995ed-ce12-4ea5-89dc-b181701d1d7b, " +
            "7549acd8-91e1-4230-833a-2f386e09b96c, " +
            "'2020-12-03 11:00:00.000', " +
            s"${UUID.randomUUID().toString}, " +
            "'verification'," +
            "'2020-12-03 19:44:44.261'," +
            "'Lana Del Rey - Bogota Concert'," +
            "'2020-12-03 19:44:43.243'," +
            "'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTg0MTI0MjYsImlhdCI6MTYwNzAyMjAyNiwianRpIjoiNDhkOGE5ZjUtMjY4Mi00YzU5LThjMDctM2NiN2VjMzVkMmE1IiwicHVycG9zZSI6IkxhbmEgRGVsIFJleSAtIEJvZ290YSBDb25jZXJ0IiwidGFyZ2V0X2lkZW50aXRpZXMiOiIqIiwicm9sZSI6InZlcmlmaWVyIn0.asqbiVNGesd6-S-tE0M7uJs6Kw2xEVMASo9M6XpSrJhXdj4qrnW-9EcgdvV2FGowrTeEFqS0zrt7LxphkQh-Wg');").stripMargin
        )
      ).foreach(x => x.forEachStatement { x => val _ = cassandra.connection.execute(x) })

      val token = Injector.get[FakeTokenCreator].user

      def uuid = "963995ed-ce12-4ea5-89dc-b181701d1d7b"
      def identity = "7549acd8-91e1-4230-833a-2f386e09b96f"

      get(s"/v1/$uuid?identity_id=$identity", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val expected = """{"version":"1.0.0","ok":true,"data":[{"id":"15f0c427-5dfb-4e15-853c-0770dd400763","ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b","identityId":"7549acd8-91e1-4230-833a-2f386e09b96f","category":"verification","description":"Lana Del Rey - Bogota Concert","tokenValue":"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTg0MTI0MjYsImlhdCI6MTYwNzAyMjAyNiwianRpIjoiNDhkOGE5ZjUtMjY4Mi00YzU5LThjMDctM2NiN2VjMzVkMmE1IiwicHVycG9zZSI6IkxhbmEgRGVsIFJleSAtIEJvZ290YSBDb25jZXJ0IiwidGFyZ2V0X2lkZW50aXRpZXMiOiIqIiwicm9sZSI6InZlcmlmaWVyIn0.asqbiVNGesd6-S-tE0M7uJs6Kw2xEVMASo9M6XpSrJhXdj4qrnW-9EcgdvV2FGowrTeEFqS0zrt7LxphkQh-Wg","day":"2020-12-03T10:00:00.000Z","occurredAt":"2020-12-03T18:44:43.243Z","createdAt":"2020-12-03T18:44:44.261Z"}]}""".stripMargin
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
    EmbeddedCassandra.truncateScript.forEachStatement(cassandra.connection.execute _)
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
