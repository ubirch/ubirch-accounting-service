package com.ubirch.job

import com.typesafe.config.ConfigFactory
import com.ubirch.models.postgres._
import com.ubirch.services.externals.{ Identity, Tenant, ThingAPI }
import com.ubirch.services.{ AcctEventsService, DailyCountResult }
import com.ubirch.{ InjectorHelper, InjectorHelperImpl, Job, TestBaseWithH2 }
import monix.eval.Task
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.duration.DurationInt

class JobTest extends TestBaseWithH2 with MockitoSugar {
  override def getInjector: InjectorHelper = new InjectorHelperImpl() {}

  "JobTest" should {
    "run successfully" in {
      val config = ConfigFactory.load()
      val flywaySupport = injector.get[FlywaySupport]
      val jobDAO = injector.get[JobDAO]
      val tenantDAO = injector.get[TenantDAO]
      val identityDAO = injector.get[IdentityDAO]
      val eventDAO = injector.get[EventDAO]
      val acctEventsService = mock[AcctEventsService]
      val thingAPI = mock[ThingAPI]

      val job = new Job(config, flywaySupport, thingAPI, jobDAO, tenantDAO, identityDAO, eventDAO, acctEventsService)

      val queryDates = List(LocalDate.now().minusDays(1))

      val tenantId = UUID.randomUUID()
      val subTenantId = UUID.randomUUID()
      val subTenant = Tenant(subTenantId.toString, "TENANT_OU_small", Map.empty[String, String], Nil, "/TENANTS_ubirch/TENANT_size/TENANT_OU_small")
      val tenant = Tenant(tenantId.toString, "TENANT_size", Map.empty[String, String], List(subTenant), "/TENANTS_ubirch/TENANT_size")
      when(thingAPI.getTenants(any[String])).thenReturn(Task(List(tenant)))
      val identityId = UUID.randomUUID().toString
      val deviceId = UUID.randomUUID().toString
      val identity = Identity(identityId, deviceId, "DESC", Map.empty[String, String], Some(tenantId))
      when(thingAPI.getTenantIdentities(any[String], any[UUID])).thenReturn(Task(List(identity)))

      when(acctEventsService.dailyCount(any[UUID], any[UUID], ArgumentMatchers.eq("anchoring"), any[LocalDate], any[Option[String]]))
        .thenReturn(Task(DailyCountResult(UUID.fromString(identityId), tenantId, "anchoring", LocalDate.now(), None, 2022, 6, 1, 1)))
      when(acctEventsService.dailyCount(any[UUID], any[UUID], ArgumentMatchers.eq("upp_verification"), any[LocalDate], any[Option[String]]))
        .thenReturn(Task(DailyCountResult(UUID.fromString(identityId), tenantId, "upp_verification", LocalDate.now(), None, 2022, 6, 1, 2)))
      when(acctEventsService.dailyCount(any[UUID], any[UUID], ArgumentMatchers.eq("uvs_verification"), any[LocalDate], any[Option[String]]))
        .thenReturn(Task(DailyCountResult(UUID.fromString(identityId), tenantId, "uvs_verification", LocalDate.now(), None, 2022, 6, 1, 3)))

      await(job.start(queryDates), 1.seconds)

      val jobRow = await(jobDAO.getLatestJob, 2.seconds)
      assert(jobRow.isDefined)
      assert(jobRow.get.success.isDefined)
      assert(jobRow.get.success.get)

      val subTenantRows = await(tenantDAO.getSubTenants, 2.seconds)
      assert(subTenantRows.length == 1)
      assert(subTenantRows.head.id == subTenantId)

      val identityRow = await(identityDAO.getByTenantId(tenantId), 2.seconds)
      assert(identityRow.length == 1)

      val eventRow = await(eventDAO.getByIdentityId(UUID.fromString(identityId)), 2.seconds)
      assert(eventRow.length == 3)
      val eventCategories = eventRow.map(_.category)
      assert(eventCategories.contains("anchoring"))
      assert(eventCategories.contains("upp_verification"))
      assert(eventCategories.contains("uvs_verification"))

      assert(eventRow.filter(_.category == "anchoring").head.count == 1)
      assert(eventRow.filter(_.category == "upp_verification").head.count == 2)
      assert(eventRow.filter(_.category == "uvs_verification").head.count == 3)
    }
  }
}
