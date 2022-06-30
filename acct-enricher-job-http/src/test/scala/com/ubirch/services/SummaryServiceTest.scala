package com.ubirch.services

import com.ubirch.TestBase
import com.ubirch.models.postgres.{ EventDAO, EventRow, TenantDAO, TenantRow }
import monix.eval.Task
import monix.execution.atomic.Atomic
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate
import java.util.{ Date, UUID }
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Random, Success, Try }

class SummaryServiceTest extends TestBase with MockitoSugar {

  "SummaryService" should {
    "fail if the given category is unknown" in {
      val eventDAO = mock[EventDAO]
      val tenantDAO = mock[TenantDAO]
      val service = new DefaultSummaryService(eventDAO, tenantDAO)

      val invoiceId = UUID.randomUUID()
      val invoiceDate = LocalDate.now()
      val from = LocalDate.now().minusDays(30)
      val to = LocalDate.now()
      val tenantId = UUID.randomUUID()

      Try(await(service.get(invoiceId.toString, invoiceDate, from, to, "orderRef", tenantId, Some("wrongCategory")), 2.seconds)) match {
        case Failure(_) => succeed
        case Success(_) => fail("Must be failed with IllegalArgumentException")
      }
    }

    "serve the consumption report successfully" in {
      val eventDAO = mock[EventDAO]
      val tenantDAO = mock[TenantDAO]
      val service = new DefaultSummaryService(eventDAO, tenantDAO)

      val invoiceId = UUID.randomUUID()
      val invoiceDate = LocalDate.now()
      val from = LocalDate.now().minusDays(30)
      val to = LocalDate.now()

      val tenantId = UUID.randomUUID()
      val subTenantId = UUID.randomUUID()
      val tenant = TenantRow(tenantId, None, "TENANT_OU_small", "/TENANTS_ubirch/TENANT_size", Some("size"), None, None, None, Map.empty[String, String], new Date(), new Date())
      val subTenant = TenantRow(subTenantId, Some(tenantId), "TENANT_OU_small", "/TENANTS_ubirch/TENANT_size/TENANT_OU_small", Some("size"), None, None, None, Map.empty[String, String], new Date(), new Date())

      val anchoringCount = Atomic(0)
      val uppVerificationCount = Atomic(0)
      val uvsVerificationCount = Atomic(0)

      val identitiesAnchoringCounts = (1 to 5).map(_ => {
        val count = Random.nextInt(20)
        anchoringCount.increment(count)
        EventRow(UUID.randomUUID(), tenantId, "anchoring", LocalDate.now(), count, new Date(), new Date())
      }).toList
      val identitiesUppVerificationCounts = (1 to 5).map(_ => {
        val count = Random.nextInt(20)
        uppVerificationCount.increment(count)
        EventRow(UUID.randomUUID(), tenantId, "upp_verification", LocalDate.now(), count, new Date(), new Date())
      }).toList
      val identitiesUvsVerificationCounts = (1 to 5).map(_ => {
        val count = Random.nextInt(20)
        uvsVerificationCount.increment(count)
        EventRow(UUID.randomUUID(), tenantId, "uvs_verification", LocalDate.now(), count, new Date(), new Date())
      }).toList

      when(tenantDAO.getTenant(any[UUID])).thenReturn(Task(Option(tenant)))
      when(tenantDAO.getSubTenants(any[UUID])).thenReturn(Task(List(subTenant)))
      when(eventDAO.get(any[UUID], ArgumentMatchers.eq(Some("anchoring")), any[LocalDate], any[LocalDate]))
        .thenReturn(Task(identitiesAnchoringCounts))
      when(eventDAO.get(any[UUID], ArgumentMatchers.eq(Some("upp_verification")), any[LocalDate], any[LocalDate]))
        .thenReturn(Task(identitiesUppVerificationCounts))
      when(eventDAO.get(any[UUID], ArgumentMatchers.eq(Some("uvs_verification")), any[LocalDate], any[LocalDate]))
        .thenReturn(Task(identitiesUvsVerificationCounts))

      val resultAnchoring = await(service.get(invoiceId.toString, invoiceDate, from, to, "orderRef", tenantId, Some("anchoring")), 2.seconds)
      val resultUppVerification = await(service.get(invoiceId.toString, invoiceDate, from, to, "orderRef", tenantId, Some("upp_verification")), 2.seconds)
      val resultUvsVerification = await(service.get(invoiceId.toString, invoiceDate, from, to, "orderRef", tenantId, Some("uvs_verification")), 2.seconds)
      assert(resultAnchoring.customers.filter(_.customerId == subTenantId.toString).head.customerDetails.head.eventQuantity == anchoringCount.get())
      assert(resultUppVerification.customers.filter(_.customerId == subTenantId.toString).head.customerDetails.head.eventQuantity == uppVerificationCount.get())
      assert(resultUvsVerification.customers.filter(_.customerId == subTenantId.toString).head.customerDetails.head.eventQuantity == uvsVerificationCount.get())
    }
  }
}
