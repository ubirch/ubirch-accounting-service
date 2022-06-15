package com.ubirch

import com.ubirch.ConfPaths.JobConfPaths
import com.ubirch.models.postgres.{ FlywaySupport, TenantDAO, TenantRow }
import com.ubirch.services.externals.{ Tenant, ThingAPI }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import net.logstash.logback.argument.StructuredArguments.v

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
    tenantDAO: TenantDAO
)(implicit scheduler: Scheduler) extends LazyLogging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val home: String = System.getProperty("user.home")

  val ubirchToken = config.getString(JobConfPaths.UBIRCH_TOKEN)

  logger.info(s"job_version=${Job.version} user_home=$home")

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
      _ <- Task.sequence(subTenants.map { st => thingAPI.getTenantDevices(ubirchToken, st.id) }).map(_.flatten)
    } yield ())
      .map { _ =>
        logger.info(s"job_step($jobId)=finished OK", v("job_id", jobId))
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
