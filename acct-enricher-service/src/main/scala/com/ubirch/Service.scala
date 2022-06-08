package com.ubirch

import com.ubirch.models.FlywaySupport

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }

import java.util.TimeZone
import javax.inject.{ Inject, Singleton }

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Service @Inject() (flywaySupport: FlywaySupport)(implicit scheduler: Scheduler) extends LazyLogging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val home: String = System.getProperty("user.home")

  logger.info(s"service_version=${Service.version} user_home=$home")

  def start(): CancelableFuture[Unit] = {

    (for {
      _ <- Task.delay(flywaySupport.migrateWhenOn())
    } yield ()).onErrorRecover {
      case e: Exception =>
        logger.error("error_starting=" + e.getClass.getCanonicalName + " - " + e.getMessage, e)
        sys.exit(1)
    }.runToFuture

  }

}

object Service extends Boot(List(new Binder)) {
  final val version = "0.7.6"
  def main(args: Array[String]): Unit = * {
    get[Service].start()
  }
}
