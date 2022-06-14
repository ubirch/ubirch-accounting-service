package com.ubirch

import com.ubirch.models.postgres.{ FlywaySupport, TenantDAO }
import com.ubirch.services.externals.ThingAPI

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import cats.syntax.all._
import monix.execution.{ CancelableFuture, Scheduler }

import java.util.TimeZone
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

  val ubirchToken = config.getString("")

  logger.info(s"job_version=${Job.version} user_home=$home")

  def start(): CancelableFuture[Unit] = {

    (for {
      //Check database scripts
      //Check if tenants and devices need to be updated
      //If yes, get data and store
      _ <- Task.delay(flywaySupport.migrateWhenOn())
      tenants <- thingAPI.getTenants(ubirchToken)
      _ <- tenants.traverse { t => tenantDAO.store(t) }

    } yield ())
      .map { _ =>
        logger.info("job_finished=OK")
        sys.exit(0)
      }
      .onErrorRecover {
        case e: Exception =>
          logger.error("error_starting=" + e.getClass.getCanonicalName + " - " + e.getMessage, e)
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
