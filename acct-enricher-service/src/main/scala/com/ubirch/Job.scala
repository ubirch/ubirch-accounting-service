package com.ubirch

import com.ubirch.ConfPaths.JobConfPaths
import com.ubirch.models.postgres.{ EventDAO, EventRow, FlywaySupport, IdentityDAO, IdentityRow, TenantDAO, TenantRow }
import com.ubirch.services.AcctEventsService
import com.ubirch.services.externals.{ Tenant, ThingAPI }
import com.ubirch.util.DateUtil

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.v

import java.text.SimpleDateFormat
import java.time.{ LocalDate, ZoneId }
import java.util.{ TimeZone, UUID }
import javax.inject.{ Inject, Singleton }

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Job @Inject() (
    config: Config,
    flywaySupport: FlywaySupport,
    thingAPI: ThingAPI,
    tenantDAO: TenantDAO,
    identityDAO: IdentityDAO,
    eventDAO: EventDAO,
    acctEventsService: AcctEventsService
)(implicit scheduler: Scheduler) extends LazyLogging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val home: String = System.getProperty("user.home")

  val ubirchToken: String = config.getString(JobConfPaths.UBIRCH_TOKEN)

  val cats: List[String] = List("anchoring", "upp_verification", "uvs_verification").distinct

  logger.info(s"job_version=${Job.version} user_home=$home")

  def start(queryDays: List[LocalDate]): CancelableFuture[Unit] = {

    val jobId = UUID.randomUUID()

    (for {
      _ <- Task.unit
      _ = logger.info(s"job_step($jobId)=started Ok with a query day of ${queryDays.mkString(",")}", jobIdStructuredArgument(jobId))

      _ <- Task.delay(flywaySupport.migrateWhenOn())
      _ = logger.info(s"job_step($jobId)=checked postgres db", jobIdStructuredArgument(jobId))

      tenants <- thingAPI.getTenants(ubirchToken)
      _ = logger.info(s"job_step($jobId)=got tenants", jobIdStructuredArgument(jobId))

      _ <- storeOrUpdate(tenants)
      _ = logger.info(s"job_step($jobId)=stored tenants", jobIdStructuredArgument(jobId))

      subTenants <- tenantDAO.getSubTenants
      _ = logger.info(s"job_step($jobId)=got ${subTenants.size} subtenants", jobIdStructuredArgument(jobId))

      identities <- Task.sequence(subTenants.map { st => thingAPI.getTenantIdentities(ubirchToken, st.id) }).map(_.flatten)
      identityRows <- Task.sequence(identities.map { d => identityDAO.store(IdentityRow.fromIdentity(d)) })
      _ = logger.info(s"job_step($jobId)=stored identities", jobIdStructuredArgument(jobId))

      _ <- Task.sequence(queryDays.map(queryDay => aggregateAndStore(jobId = jobId, category = "anchoring", date = queryDay, subCategory = None, identityRows)))
      _ <- Task.sequence(queryDays.map(queryDay => aggregateAndStore(jobId = jobId, category = "upp_verification", date = queryDay, subCategory = None, identityRows)))
      _ <- Task.sequence(queryDays.map(queryDay => aggregateAndStore(jobId = jobId, category = "uvs_verification", date = queryDay, subCategory = None, identityRows)))

    } yield ())
      .timed
      .map { case (duration, _) =>
        val dur = if (duration.toMinutes > 0) duration.toMinutes + " minutes" else duration.toSeconds + " seconds"
        logger.info(s"job_step($jobId)=finished OK with a duration of $dur", jobIdStructuredArgument(jobId))
        sys.exit(0)
      }
      .onErrorRecover {
        case e: Exception =>
          logger.error(s"job_step($jobId)= " + e.getClass.getCanonicalName + " - " + e.getMessage, e, jobIdStructuredArgument(jobId))
          sys.exit(1)
      }.runToFuture

  }

  private def aggregateAndStore(jobId: UUID, category: String, date: LocalDate, subCategory: Option[String], identityRows: List[IdentityRow]) = {
    val aggregationId = UUID.randomUUID()
    for {
      _ <- Task.unit
      _ = logger.info(s"job_step($jobId)=started $category aggregation($aggregationId) for date ${date.toString}", jobIdStructuredArgument(jobId), jobAggregationIdStructuredArgument(aggregationId))
      aggregationResult <- Task.sequence(identityRows.map(i => acctEventsService.dailyCount(
        identityId = i.id,
        tenantId = i.tenantId,
        category = category,
        date = date,
        subCategory = subCategory
      )))
      _ = logger.info(s"job_step($jobId)=finished $category aggregation($aggregationId) for date ${date.toString}", jobIdStructuredArgument(jobId), jobAggregationIdStructuredArgument(aggregationId))
      _ <- Task.sequence(aggregationResult.map { d => eventDAO.store(EventRow.fromDailyCountResult(d)) })
      _ = logger.info(s"job_step($jobId)=stored $category aggregation($aggregationId) for date ${date.toString}", jobIdStructuredArgument(jobId), jobAggregationIdStructuredArgument(aggregationId))
    } yield {
      ()
    }
  }

  private def jobIdStructuredArgument(jobId: UUID): StructuredArgument = v("acct_enricher.job_id", jobId)

  private def jobAggregationIdStructuredArgument(aggregationId: UUID): StructuredArgument = v("acct_enricher.job_aggregation_id", aggregationId)

  private def storeOrUpdate(tenants: List[Tenant]): Task[List[Unit]] = {
    def go(parent: Option[Tenant], tenants: List[Tenant]): Task[List[Unit]] = {
      tenants match {
        case Nil => Task.delay(Nil)
        case tenant :: other =>
          tenantDAO.store(TenantRow.fromTenant(parent, tenant))
            .flatMap(_ => go(parent, other))
            .flatMap(_ => go(Some(tenant), tenant.subTenants))
      }
    }

    go(None, tenants)

  }

}

object Job extends Boot(List(new Binder)) {
  final val version = "0.7.6"

  lazy val sdf = new SimpleDateFormat("yyyy-MM-dd")

  def main(args: Array[String]): Unit = * {
    val queryDays =
      args
        .map(sdf.parse)
        .map(x => DateUtil.dateToLocalDate(x, ZoneId.systemDefault()))
        .toList
        .distinct match {
          case Nil => List(LocalDate.now().minusDays(1))
          case other => other
        }

    get[Job].start(queryDays)
  }
}
