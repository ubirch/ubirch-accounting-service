package com.ubirch.controllers

import com.ubirch.models.Return
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.{ Awaits, EmbeddedCassandra, ExecutionContextsTests, FakeTokenCreator, InjectorHelperImpl }

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
