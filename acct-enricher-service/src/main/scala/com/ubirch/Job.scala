package com.ubirch

import com.ubirch.ConfPaths.JobConfPaths
import com.ubirch.models.postgres.{ FlywaySupport, TenantDAO, TenantRow }
import com.ubirch.services.externals.{ Tenant, ThingAPI }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import net.logstash.logback.argument.StructuredArguments.v

import java.util.{ Date, TimeZone, UUID }
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

  def store(parent: Option[Tenant], tenants: List[Tenant]): Task[Unit] = {
    tenants match {
      case Nil => Task.unit
      case tenant :: ts =>
        val newTenant = TenantRow(
          id = UUID.fromString(tenant.id),
          parentId = parent.map(_.id).map(x => UUID.fromString(x)),
          groupName = tenant.name,
          groupPath = tenant.path,
          name = tenant.attributes.get("tenant_name"),
          address = tenant.attributes.get("tenant_address"),
          representative = tenant.attributes.get("tenant_representative"),
          taxId = tenant.attributes.get("tenant_tax_id"),
          attributes = tenant.attributes,
          createdAt = new Date(),
          updatedAt = new Date()
        )

        tenantDAO
          .store(newTenant)
          .flatMap(_ => store(Option(tenant), tenant.subTenants ++ ts))
    }
  }

  def start(): CancelableFuture[Unit] = {

    val jobId = UUID.randomUUID()

    (for {
      _ <- Task.delay(flywaySupport.migrateWhenOn())
      _ = logger.info(s"job_step($jobId)=checked postgres db", v("job_id", jobId))
      tenants <- thingAPI.getTenants(ubirchToken)
      _ = logger.info(s"job_step($jobId)=got tenants", v("job_id", jobId))
      _ <- store(None, tenants)
      _ = logger.info(s"job_step($jobId)=stored tenants", v("job_id", jobId))
    } yield ())
      .map { _ =>
        logger.info(s"job_step($jobId)=finished OK", v("job_id", jobId))
        sys.exit(0)
      }
      .onErrorRecover {
        case e: Exception =>
          logger.error(s"job_step($jobId)= " + " error_starting " + e.getClass.getCanonicalName + " - " + e.getMessage, e, v("job_id", jobId))
          sys.exit(1)
      }.runToFuture

  }

}

object Job extends Boot(List(new Binder)) {
  final val version = "0.7.6"
  def main(args: Array[String]): Unit = * {
    get[Job].start()
  }
}
