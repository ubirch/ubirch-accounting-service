package com.ubirch.services.externals

import com.ubirch.ServiceException
import com.ubirch.services.lifeCycle.Lifecycle

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.reactive.Observable
import org.apache.hc.client5.http.async.methods.{ SimpleHttpRequest, SimpleHttpResponse, SimpleRequestProducer, SimpleResponseConsumer }
import org.apache.hc.client5.http.impl.async.{ CloseableHttpAsyncClient, HttpAsyncClients }
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.pool.{ PoolConcurrencyPolicy, PoolReusePolicy }
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.{ TimeValue, Timeout }

import java.nio.charset.StandardCharsets
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ Future, Promise }

trait HttpClient {
  def execute(request: SimpleHttpRequest): Future[ResponseData[Array[Byte]]]
  def executeAsTask(request: SimpleHttpRequest): Task[ResponseData[Array[Byte]]]
}

trait HttpClientStream {
  def execute(request: SimpleHttpRequest): Observable[String]
}

trait HttpClientPath {
  val HTTP_SO_TIMEOUT_CONFIG_PATH = "system.http.client.socketTimeout"
  val HTTP_CONNECTION_TIME_TO_LIVE_CONFIG_PATH = "system.http.client.connectionTtl"
  val HTTP_EVICT_IDLE_CONNECTIONS_CONFIG_PATH = "system.http.client.evictIdleConnections"
}

@Singleton
class DefaultHttpClient @Inject() (config: Config, lifecycle: Lifecycle) extends HttpClient with HttpClientPath with LazyLogging {

  final val HTTP_SO_TIMEOUT = config.getLong(HTTP_SO_TIMEOUT_CONFIG_PATH) //2
  final val HTTP_CONNECTION_TIME_TO_LIVE = config.getLong(HTTP_CONNECTION_TIME_TO_LIVE_CONFIG_PATH) //1
  final val HTTP_EVICT_IDLE_CONNECTIONS = config.getLong(HTTP_EVICT_IDLE_CONNECTIONS_CONFIG_PATH) //10

  private val ioReactorConfig: IOReactorConfig = IOReactorConfig
    .custom
    .setSoTimeout(Timeout.ofSeconds(HTTP_SO_TIMEOUT))
    .build

  private val connMr = PoolingAsyncClientConnectionManagerBuilder.create
    .setConnectionTimeToLive(TimeValue.ofSeconds(HTTP_CONNECTION_TIME_TO_LIVE))
    .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
    .setConnPoolPolicy(PoolReusePolicy.LIFO)
    .build

  private val httpclient: CloseableHttpAsyncClient = HttpAsyncClients
    .custom
    .evictExpiredConnections()
    .evictIdleConnections(TimeValue.ofSeconds(HTTP_EVICT_IDLE_CONNECTIONS))
    .setConnectionManager(connMr)
    .setIOReactorConfig(ioReactorConfig)
    .build

  httpclient.start()

  override def execute(request: SimpleHttpRequest): Future[ResponseData[Array[Byte]]] = {
    val promise = Promise[ResponseData[Array[Byte]]]()
    httpclient.execute(
      SimpleRequestProducer.create(request),
      SimpleResponseConsumer.create(),
      new FutureCallback[SimpleHttpResponse] {
        override def completed(result: SimpleHttpResponse): Unit = {

          if (result.getCode > 299) {
            logger.error(
              s"error_sending_${request.getMethod.toLowerCase}_to=" + request.getUri.toString +
                " status=" + result.getCode +
                " body=" + new String(result.getBodyBytes, StandardCharsets.UTF_8)
            )
          }

          promise.success(
            ResponseData(
              result.getCode,
              result.getHeaders.map(x => (x.getName, List(x.getValue))).toMap,
              result.getBodyBytes
            )
          )

        }

        override def failed(e: Exception): Unit = {
          logger.error(s"error_in_execute (${request.getUri.toString}) -> ", e)
          promise.failure(InvalidHttpException("error_in_execute", ServiceException.exceptionToString(e)))
        }

        override def cancelled(): Unit = {
          logger.error("error_in_execute_cancelled")
          promise.failure(InvalidHttpException("error_in_execute_cancelled", "Cancelled"))
        }
      }
    )
    promise.future
  }

  def executeAsTask(request: SimpleHttpRequest): Task[ResponseData[Array[Byte]]] = Task.defer {
    Task.fromFuture(execute(request))
  }

  lifecycle.addStopHook { () =>
    logger.info("Shutting Http Client Service")
    Future.successful(httpclient.close(CloseMode.GRACEFUL))
  }

}
