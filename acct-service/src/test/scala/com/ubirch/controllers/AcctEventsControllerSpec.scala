package com.ubirch.controllers

import com.github.nosan.embedded.cassandra.cql.StringCqlScript
import com.ubirch._
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.util.cassandra.test.EmbeddedCassandraBase
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
  with EmbeddedCassandraBase
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  private lazy val Injector = new InjectorHelperImpl() {}

  "Acct Events Service" must {

    "get OK with result with date" in {
      List(
        new StringCqlScript(
          "INSERT INTO acct_system.acct_events (identity_id, category, year, month, day, hour, sub_category, id, occurred_at) " +
            "VALUES (12539f76-c7e9-47d6-b37b-4b59380721ac, 'verification', 2022, 2, 9, 11, 'entry-b', 000014a4-02f0-4c69-96f0-d85de7cb9dd8, '2022-02-09 11:45:43.401');"
        ),
        new StringCqlScript(
          "INSERT INTO acct_system.acct_events (identity_id, category, year, month, day, hour, sub_category, id, occurred_at) " +
            "VALUES (12539f76-c7e9-47d6-b37b-4b59380721ac, 'verification', 2022, 2, 9, 12, 'entry-a', 000014a4-02f0-4c69-96f0-d85de7cb9dd8, '2022-02-09 12:45:43.401');"
        )
      ).foreach(x => x.forEachStatement { x => val _ = cassandra.session.execute(x) })

      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"

      def identity = "12539f76-c7e9-47d6-b37b-4b59380721ac"
      def date = "2022-02-01"
      val category = "verification"

      var subcategory = "entry-b"

      get(s"/v1/$identity?date=$date&cat=$category&sub_cat=$subcategory", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(200)
        val expected = """{"version":"0.7.14","ok":true,"data":[{"year":2022,"month":2,"count":1}]}""".stripMargin
        assert(body == expected)
      }

      subcategory = "entry-a"

      get(s"/v1/$identity?date=$date&cat=$category&sub_cat=$subcategory", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(200)
        val expected = """{"version":"0.7.14","ok":true,"data":[{"year":2022,"month":2,"count":1}]}""".stripMargin
        assert(body == expected)
      }

      // All subcats

      get(s"/v1/$identity?date=$date&cat=$category", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(200)
        val expected = """{"version":"0.7.14","ok":true,"data":[{"year":2022,"month":2,"count":2}]}""".stripMargin
        assert(body == expected)
      }

    }

    "fail when no access token provided: get" in {

      def uuid = UUID.randomUUID()
      get(s"/v1/$uuid") {
        status should equal(403)
        assert(body == """{"version":"0.7.14","ok":false,"errorType":"AuthenticationError","errorMessage":"Forbidden"}""")
      }

    }

    "fail when required params not provided: get" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      def identity = "12539f76-c7e9-47d6-b37b-4b59380721ac"

      info("identity_id")
      get(s"/v1/123", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(body == """{"version":"0.7.14","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid identity_id: wrong identity param: 123"}""")
      }

      info("category")
      get(s"/v1/$identity", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(body == """{"version":"0.7.14","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid cat: wrong cat param"}""")
      }

      info("date")
      get(s"/v1/$identity?cat=default", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(body == """{"version":"0.7.14","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Date: Use yyyy-MM this format"}""")
      }

    }

  }

  override protected def beforeEach(): Unit = {
    CollectorRegistry.defaultRegistry.clear()
    EmbeddedCassandra.truncateScript.forEachStatement { x => val _ = cassandra.session.execute(x) }
  }

  protected override def afterAll(): Unit = {
    cassandra.stop()
    super.afterAll()
  }

  protected override def beforeAll(): Unit = {

    CollectorRegistry.defaultRegistry.clear()
    cassandra.startAndCreateDefaults(EmbeddedCassandra.creationScripts)

    lazy val pool = Injector.get[PublicKeyPoolService]
    await(pool.init, 2 seconds)

    lazy val tokenController = Injector.get[AcctEventsController]

    addServlet(tokenController, "/*")

    super.beforeAll()
  }
}
