package com.ubirch

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.services.kafka.AcctManager
import com.ubirch.services.rest.RestService
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import monix.execution.Scheduler

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Service @Inject() (restService: RestService, acctManager: AcctManager, publicKeyPoolService: PublicKeyPoolService)(implicit scheduler: Scheduler) extends LazyLogging {

  def start(): Unit = {

    publicKeyPoolService.init.doOnFinish {
      case Some(e) =>
        Task.delay(logger.error("error_loading_keys", e))
      case None =>
        Task.delay {
          acctManager.start()
          restService.start()
        }
    }.runToFuture

    val cd = new CountDownLatch(1)
    cd.await()
  }

}

object Service extends Boot(List(new Binder)) {
  def main(args: Array[String]): Unit = * {
    get[Service].start()
  }
}
