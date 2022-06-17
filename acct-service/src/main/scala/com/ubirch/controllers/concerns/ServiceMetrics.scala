package com.ubirch.controllers.concerns

import io.prometheus.client.Counter
import monix.execution.CancelableFuture
import org.scalatra.ActionResult

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

trait ServiceMetrics {

  def service: String

  def successCounter: Counter

  def errorCounter: Counter

  def count(method: String)(cf: CancelableFuture[ActionResult])(implicit ec: ExecutionContext): CancelableFuture[ActionResult] = {
    cf.onComplete {
      case Success(ar) =>
        if (ar.status <= 299) successCounter.labels(service, method).inc()
        else errorCounter.labels(service, method).inc()
      case Failure(_) => errorCounter.labels(service, method).inc()
    }
    cf
  }

}
