package com.ubirch.controllers

import com.ubirch.models.postgres.{ DefaultEventDAO, DefaultTenantDAO, EventRow, TenantRow }
import com.ubirch.{ InjectorHelper, InjectorHelperImpl, TestBaseWithH2 }
import monix.execution.atomic.Atomic
import org.json4s.Formats
import org.scalatra.test.scalatest.ScalatraWordSpec

import java.time.{ Instant, LocalDate }
import java.util.{ Date, UUID }
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class AcctEventsEnricherControllerSpec extends TestBaseWithH2
  with ScalatraWordSpec {

  override def getInjector: InjectorHelper = new InjectorHelperImpl() {}
  implicit val formats: Formats = getInjector.get[Formats]
  val version = "0.7.14"

  private val ContentTypeKey = "Content-Type"
  private val ContentTypeJson = "application/json"

  "AcctEventsEnricherController - v1" must {
    "fail when 'token' is not provided" in {
      get("/v1") {
        status should equal(403)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AuthenticationError","errorMessage":"Forbidden"}""")
      }
    }

    "fail when 'tenant id' is not provided" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"

      get("/v1", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Tenant Id: wrong tenant_id param: "}""")
      }
    }

    "fail when 'tenant id' is not valid uuid" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = "tenantId"

      get(s"/v1?tenant_id=$tenantId", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Tenant Id: wrong tenant_id param: tenantId"}""")
      }
    }

    "fail when 'orderRef' is not provided" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()

      get(s"/v1?tenant_id=$tenantId", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Order ref: wrong order_ref param"}""")
      }
    }

    "fail when 'invoice id' is not provided" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Invoice Id: wrong invoice id param: "}""")
      }
    }

    "fail when 'invoice date' is not provided" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category&invoice_id=$invoiceId", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Invoice Date: Use yyyy-MM-dd this format: None.get"}""")
      }
    }

    "fail when 'invoice date' is not valid" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01"

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Invoice Date: Use yyyy-MM-dd this format: Unparseable date: \\"$invoiceDate\\""}""")
      }
    }

    "fail if 'from' is provided but 'to' not" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val from = "2021-12-01"

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&from=$from", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Range Definition: Start requires End"}""")
      }
    }

    "fail if 'to' is provided but 'from' not" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val to = "2021-12-01"

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&to=$to", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Range Definition: End requires Start"}""")
      }
    }

    "fail when invalid range defined" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val to = "2021-12-01"
      val from = "2022-01-01"

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&from=$from&to=$to", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Invalid Range Definition: From must be before To"}""")
      }
    }

    "fail when the given tenant not found" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val from = "2021-12-01"
      val to = "2022-01-01"

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&category=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&from=$from&to=$to", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(404)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"NotFound","errorMessage":"Unknown tenant: $tenantId"}""")
      }
    }

    "fail when unknown category provided" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "category"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val from = "2021-12-01"
      val to = "2022-01-01"

      val tenantRow = TenantRow(
        id = tenantId,
        parentId = None,
        groupName = "TENANT",
        groupPath = "TENANT_ubirch/TENANT",
        name = None,
        address = None,
        representative = None,
        taxId = None,
        attributes = Map.empty[String, String],
        createdAt = Date.from(Instant.now),
        updatedAt = Date.from(Instant.now)
      )

      val tenantDAO = injector.get[DefaultTenantDAO]
      await(tenantDAO.insertTenant(tenantRow), 1 seconds)

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&cat=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&from=$from&to=$to", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(400)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":false,"errorType":"AcctEventQueryError","errorMessage":"Sorry, there is something invalid in your request: Unknown category: anchoring, upp_verification, uvs_verification"}""")
      }
    }

    "return empty response if there is no subtenant available" in {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val from = "2021-12-01"
      val to = "2022-01-01"

      val tenantRow = TenantRow(tenantId, None, "TENANT_OU_small", "/TENANTS_ubirch/TENANT_size", Some("size"), None, None, None, Map.empty[String, String], new Date(), new Date())

      val tenantDAO = injector.get[DefaultTenantDAO]
      await(tenantDAO.insertTenant(tenantRow), 1 seconds)

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&cat=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&from=$from&to=$to", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(200)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":true,"data":{"schemaVersion":"V1.00","supplierName":"ubirch GmbH","supplierId":"ubirch GmbH","invoiceId":"$invoiceId","invoiceDate":"$invoiceDate","customers":[]}}""")
      }
    }

    "return account events summary successfully" in {
      val fromLocalDate = LocalDate.now().minusDays(30)
      val toLocalDate = LocalDate.now().plusDays(1)
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL2RhdGEuZGV2LnViaXJjaC5jb20iXSwiaWF0IjoxNjQ0NDg1OTA5LCJqdGkiOiI0NmZkYzczNi0zOWJjLTQ1NDUtYWVhNi1kZTgzNjBhYjJmNWYiLCJzY3AiOlsidGhpbmc6Z2V0aW5mbyIsInRoaW5nOnN0b3JlZGF0YSJdLCJwdXIiOiJBY2NvdW50aW5nIFNlcnZpY2UiLCJ0Z3AiOltdLCJ0aWQiOlsiMTI1MzlmNzYtYzdlOS00N2Q2LWIzN2ItNGI1OTM4MDcyMWFjIl0sIm9yZCI6W119.AE1mMNKiq9j9P-_U0kan7Vi3hW7dRVs-aQ-nFRMqNEheTOdQ4RDKx7CmpsbdoBoo8koN2TrRVkEXQLr7X1zgLg"
      val tenantId = UUID.randomUUID()
      val subTenantId = UUID.randomUUID()
      val orderRef = "ubirch"
      val category = "anchoring"
      val invoiceId = UUID.randomUUID()
      val invoiceDate = "2022-01-01"
      val from = s"${fromLocalDate.getYear}-${fromLocalDate.getMonth.getValue}-${fromLocalDate.getDayOfMonth}"
      val to = s"${toLocalDate.getYear}-${toLocalDate.getMonth.getValue}-${toLocalDate.getDayOfMonth}"
      val subTenantName = "size"

      val tenantRow = TenantRow(tenantId, None, "TENANT_OU_small", "/TENANTS_ubirch/TENANT_size", Some("size"), None, None, None, Map.empty[String, String], new Date(), new Date())
      val subTenantRow = TenantRow(subTenantId, Some(tenantId), "TENANT_OU_small", "/TENANTS_ubirch/TENANT_size/TENANT_OU_small", Some(subTenantName), None, None, None, Map.empty[String, String], new Date(), new Date())

      val tenantDAO = injector.get[DefaultTenantDAO]
      await(tenantDAO.insertTenant(tenantRow), 1 seconds)
      await(tenantDAO.insertTenant(subTenantRow), 1 seconds)

      val eventDAO = injector.get[DefaultEventDAO]
      val anchoringCount = Atomic(0)

      (1 to 5).map(_ => {
        val count = Random.nextInt(20)
        anchoringCount.increment(count)
        await(eventDAO.insertEvent(EventRow(UUID.randomUUID(), subTenantId, "anchoring", LocalDate.now(), count, new Date(), new Date())), 1 seconds)
      }).toList

      get(s"/v1?tenant_id=$tenantId&order_ref=$orderRef&cat=$category&invoice_id=$invoiceId&invoice_date=$invoiceDate&from=$from&to=$to", headers = Map("authorization" -> s"bearer $token")) {
        status should equal(200)
        assert(response.header.contains(ContentTypeKey))
        response.header.get(ContentTypeKey).foreach { contentType =>
          assert(contentType.contains(ContentTypeJson))
        }
        assert(body == s"""{"version":"$version","ok":true,"data":{"schemaVersion":"V1.00","supplierName":"ubirch GmbH","supplierId":"ubirch GmbH","invoiceId":"$invoiceId","invoiceDate":"2022-01-01","customers":[{"customerId":"$subTenantId","customerName":"$subTenantName","customerDetails":[{"eventType":"$category","eventQuantity":${anchoringCount.get()}}]}]}}""")
      }
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  protected override def afterAll(): Unit = {
    super.afterAll()
  }

  protected override def beforeAll(): Unit = {
    lazy val controller = getInjector.get[AcctEventsEnricherController]

    addServlet(controller, "/*")

    migrate(ctx)
    super.beforeAll()
  }
}
