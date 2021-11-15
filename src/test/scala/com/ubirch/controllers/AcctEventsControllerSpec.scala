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

  "Acct Events Service" must {

    "create OK" in {

      val token = Injector.get[FakeTokenCreator].user

      def uuid = UUID.randomUUID()

      get(s"/v1/$uuid", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
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
