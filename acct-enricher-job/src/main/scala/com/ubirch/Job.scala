package com.ubirch

import com.ubirch.ConfPaths.JobConfPaths
import com.ubirch.models.postgres.{ EventDAO, EventRow, FlywaySupport, IdentityDAO, IdentityRow, JobDAO, JobRow, TenantDAO, TenantRow }
import com.ubirch.services.AcctEventsService
import com.ubirch.services.externals.{ Tenant, ThingAPI }
import com.ubirch.util.{ DateUtil, TaskHelpers }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.v

import java.time.{ LocalDate, ZoneId }
import java.util.{ TimeZone, UUID }
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Job @Inject() (
    config: Config,
    flywaySupport: FlywaySupport,
    thingAPI: ThingAPI,
    jobDAO: JobDAO,
    tenantDAO: TenantDAO,
    identityDAO: IdentityDAO,
    eventDAO: EventDAO,
    acctEventsService: AcctEventsService
)(implicit scheduler: Scheduler) extends TaskHelpers with LazyLogging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val home: String = System.getProperty("user.home")

  val ubirchToken: String = config.getString(JobConfPaths.UBIRCH_TOKEN)

  val cats: List[String] = List("anchoring", "upp_verification", "uvs_verification").distinct

  logger.info(s"job_version=${Job.version} user_home=$home")

  def start(queryDays: List[LocalDate]): CancelableFuture[Unit] = {

    val job = JobRow(queryDays)

    (for {
      _ <- preChecks(job, queryDays)
      _ <- jobDAO.store(job)
      _ <- getAndStoreOrUpdateTenants(job)
      subTenants <- getSubTenants(job)
      identityRows <- getAndStoreOrUpdateIdentities(job, subTenants)

      _ <- Task.sequence(queryDays.map(queryDay => aggregateAndStore(job = job, category = "anchoring", date = queryDay, subCategory = None, identityRows)))
      _ <- Task.sequence(queryDays.map(queryDay => aggregateAndStore(job = job, category = "upp_verification", date = queryDay, subCategory = None, identityRows)))
      _ <- Task.sequence(queryDays.map(queryDay => aggregateAndStore(job = job, category = "uvs_verification", date = queryDay, subCategory = None, identityRows)))

    } yield ())
      .timed
      .doOnFinish { error => jobDAO.store(job.end(success = error.isEmpty, error.map(e => ServiceException.exceptionToString(e)))).map(_ => ()) }
      .map { case (duration, _) =>
        val dur = if (duration.toMinutes > 0) duration.toMinutes + " minutes" else duration.toSeconds + " seconds"
        logger.info(s"job_step(${job.id})=finished OK with a duration of $dur", jobIdStructuredArgument(job))
      }
      .onErrorRecover {
        case e: Exception =>
          logger.error(s"job_step(${job.id})=finished NOK due to: " + e.getClass.getCanonicalName + " - " + e.getMessage, e, jobIdStructuredArgument(job))
      }.runToFuture

  }

  private def preChecks(job: JobRow, queryDays: List[LocalDate]) = {
    for {
      _ <- Task.delay(flywaySupport.migrateWhenOn())
      _ = logger.info(s"job_step(${job.id})=checked postgres db", jobIdStructuredArgument(job))
      _ = logger.info(s"job_step(${job.id})=started Ok with a query day of ${queryDays.mkString(",")}", jobIdStructuredArgument(job))
      _ <- earlyResponseIf(ubirchToken.isEmpty)(new IllegalArgumentException("ubirch token can't be empty"))
    } yield {
      ()
    }
  }

  private def getAndStoreOrUpdateTenants(job: JobRow) = {
    for {
      tenants <- thingAPI.getTenants(ubirchToken)
      _ = logger.info(s"job_step(${job.id})=got tenants", jobIdStructuredArgument(job))
      _ <- storeOrUpdate(tenants)
      _ = logger.info(s"job_step(${job.id})=stored tenants", jobIdStructuredArgument(job))
    } yield {
      tenants
    }
  }

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

  private def getSubTenants(job: JobRow) =
    for {
      subTenants <- tenantDAO.getSubTenants
      _ = logger.info(s"job_step(${job.id})=got ${subTenants.size} subtenants", jobIdStructuredArgument(job))
    } yield {
      subTenants
    }

  private def jobIdStructuredArgument(job: JobRow): StructuredArgument = v("acct_enricher.job_id", job.id)

  private def getAndStoreOrUpdateIdentities(job: JobRow, subTenants: List[TenantRow]) =
    for {
      identities <- Task.sequence(subTenants.map { st => thingAPI.getTenantIdentities(ubirchToken, st.id) }).map(_.flatten)
      identityRows <- Task.sequence(identities.map { d => identityDAO.store(IdentityRow.fromIdentity(d)) })
      _ = logger.info(s"job_step(${job.id})=stored identities", jobIdStructuredArgument(job))
    } yield {
      identityRows
    }

  private def aggregateAndStore(job: JobRow, category: String, date: LocalDate, subCategory: Option[String], identityRows: List[IdentityRow]) = {
    val aggregationId = UUID.randomUUID()
    for {
      _ <- Task.unit
      _ = logger.info(s"job_step(${job.id})=started $category aggregation($aggregationId) for date ${date.toString}", jobIdStructuredArgument(job), jobAggregationIdStructuredArgument(aggregationId))
      aggregationResult <- Task.sequence(identityRows.map(i => acctEventsService.dailyCount(
        identityId = i.id,
        tenantId = i.tenantId,
        category = category,
        date = date,
        subCategory = subCategory
      )))
      _ = logger.info(s"job_step(${job.id})=finished $category aggregation($aggregationId) for date ${date.toString}", jobIdStructuredArgument(job), jobAggregationIdStructuredArgument(aggregationId))
      _ <- Task.sequence(aggregationResult.map { d => eventDAO.store(EventRow.fromDailyCountResult(d)) })
      _ = logger.info(s"job_step(${job.id})=stored $category aggregation($aggregationId) for date ${date.toString}", jobIdStructuredArgument(job), jobAggregationIdStructuredArgument(aggregationId))
    } yield {
      ()
    }
  }

  private def jobAggregationIdStructuredArgument(aggregationId: UUID): StructuredArgument = v("acct_enricher.job_aggregation_id", aggregationId)

}

object Job extends Boot(List(new Binder)) {
  final val version = "0.7.7"

  def main(args: Array[String]): Unit = * {
    val queryDays =
      args
        .map(DateUtil.`yyyy-MM-dd_NotLenient`.parse)
        .map(x => DateUtil.dateToLocalDate(x, ZoneId.systemDefault()))
        .toList
        .distinct match {
          case Nil => List(LocalDate.now().minusDays(1))
          case other => other
        }

    implicit val ec: ExecutionContext = get[ExecutionContext]
    get[Job].start(queryDays).onComplete {
      case Failure(_) => sys.exit(1)
      case Success(_) => sys.exit(0)
    }
  }
}
