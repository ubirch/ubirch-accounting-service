package com.ubirch

import com.ubirch.ConfPaths.JobConfPaths
import com.ubirch.models.postgres.{ FlywaySupport, IdentityDAO, IdentityRow, TenantDAO, TenantRow }
import com.ubirch.services.AcctEventsService
import com.ubirch.services.externals.{ Tenant, ThingAPI }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import net.logstash.logback.argument.StructuredArguments.v

import java.time.LocalDate
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
    acctEventsService: AcctEventsService
)(implicit scheduler: Scheduler) extends LazyLogging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val home: String = System.getProperty("user.home")

  val ubirchToken = config.getString(JobConfPaths.UBIRCH_TOKEN)

  logger.info(s"job_version=${Job.version} user_home=$home")

  val cats = List("anchoring", "upp_verification", "uvs_verification")

  def start(): CancelableFuture[Unit] = {

    val jobId = UUID.randomUUID()

    (for {
      _ <- Task.delay(flywaySupport.migrateWhenOn())
      _ = logger.info(s"job_step($jobId)=checked postgres db", v("job_id", jobId))

      tenants <- thingAPI.getTenants(ubirchToken)
      _ = logger.info(s"job_step($jobId)=got tenants", v("job_id", jobId))

      _ <- store(tenants)
      _ = logger.info(s"job_step($jobId)=stored tenants", v("job_id", jobId))

      subTenants <- tenantDAO.getSubTenants
      _ = logger.info(s"job_step($jobId)=got ${subTenants.size} subtenants", v("job_id", jobId))

      identities <- Task.sequence(subTenants.map { st => thingAPI.getTenantIdentities(ubirchToken, st.id) }).map(_.flatten)
      identityRows <- Task.sequence(identities.map { d => identityDAO.store(IdentityRow.fromIdentity(d)) })
      _ = logger.info(s"job_step($jobId)=stored identities", v("job_id", jobId))

      monthlyResultsAnchoringFiber <- Task.sequence(identityRows.map(i => acctEventsService.monthCount(
        identityId = i.id,
        category = "anchoring",
        date = LocalDate.now(),
        subCategory = None
      ))).start
      _ = logger.info(s"job_step($jobId)=started monthlyResultsAnchoring", v("job_id", jobId))

      monthlyResultsUPPVerificationFiber <- Task.sequence(identityRows.map(i => acctEventsService.monthCount(
        identityId = i.id,
        category = "upp_verification",
        date = LocalDate.now(),
        subCategory = None
      ))).start
      _ = logger.info(s"job_step($jobId)=started monthlyResultsUPPVerification", v("job_id", jobId))

      monthlyResultsUVSVerificationFiber <- Task.sequence(identityRows.map(i => acctEventsService.monthCount(
        identityId = i.id,
        category = "uvs_verification",
        date = LocalDate.now(),
        subCategory = None
      ))).start
      _ = logger.info(s"job_step($jobId)=started monthlyResultsUVSVerification", v("job_id", jobId))

      monthlyResultsAnchoring <- monthlyResultsAnchoringFiber.join
      monthlyResultsUPPVerification <- monthlyResultsUPPVerificationFiber.join
      monthlyResultsUVSVerification <- monthlyResultsUVSVerificationFiber.join

      _ = println(monthlyResultsAnchoring)
      _ = println(monthlyResultsUPPVerification)
      _ = println(monthlyResultsUVSVerification)

    } yield ())
      .timed
      .map { case (duration, _) =>
        logger.info(s"job_step($jobId)=finished OK with a duration of ${duration.toMinutes} minutes", v("job_id", jobId))
        sys.exit(0)
      }
      .onErrorRecover {
        case e: Exception =>
          logger.error(s"job_step($jobId)= " + e.getClass.getCanonicalName + " - " + e.getMessage, e, v("job_id", jobId))
          sys.exit(1)
      }.runToFuture

  }

  private def store(tenants: List[Tenant]): Task[List[Unit]] = {
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
  def main(args: Array[String]): Unit = * {
    get[Job].start()
  }
}
