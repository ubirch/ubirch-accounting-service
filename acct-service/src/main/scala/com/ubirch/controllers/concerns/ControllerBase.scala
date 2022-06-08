package com.ubirch.controllers.concerns

import com.ubirch.models.NOK

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import org.apache.commons.compress.utils.IOUtils
import org.json4s.JsonAST.JValue
import org.scalatra._
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.SwaggerSupport

import java.io.{ ByteArrayInputStream, FileOutputStream }
import java.nio.charset.{ Charset, StandardCharsets }
import java.util.Date
import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse, HttpServletResponseWrapper }
import javax.servlet.{ ReadListener, ServletInputStream }
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.control.NoStackTrace

/**
  * Represents a customized ServletInputStream that allows to cache the body of a request.
  * This trait is very important to be able to re-consume the body in case of need.
  * @param cachedBody Represents the InputStream as bytes.
  * @param raw Represents the raw ServletInputStream
  */
class CachedBodyServletInputStream(cachedBody: Array[Byte], raw: ServletInputStream) extends ServletInputStream {

  private val cachedInputStream = new ByteArrayInputStream(cachedBody)

  override def isFinished: Boolean = cachedInputStream.available() == 0
  override def isReady: Boolean = true
  override def setReadListener(readListener: ReadListener): Unit = raw.setReadListener(readListener)

  override def read(): Int = cachedInputStream.read()
  override def read(b: Array[Byte]): Int = read(b, 0, b.length)
  override def read(b: Array[Byte], off: Int, len: Int): Int = cachedInputStream.read(b, off, len)

}

/***
 * Represents a customized HttpServletRequest that allows us to decorate the original object with extra info
 * or extra functionality.
 * Initially, it supports the re-consumption of the body stream
 * @param httpServletRequest Represents the original Request
 */
class ServiceRequest(httpServletRequest: HttpServletRequest) extends HttpServletRequestWrapper(httpServletRequest) {

  val cachedBody: Array[Byte] = IOUtils.toByteArray(httpServletRequest.getInputStream)

  val cachedHeaders: Map[String, String] = httpServletRequest.getHeaderNames.asScala.toList.map(x => Map(x -> httpServletRequest.getHeader(x))).foldLeft(Map.empty[String, String])((a, b) => a ++ b)

  def findHeader(header: String): Option[String] = cachedHeaders.find(_._1.toLowerCase == header.toLowerCase).map(_._2)

  override def getInputStream: ServletInputStream = {
    new CachedBodyServletInputStream(cachedBody, httpServletRequest.getInputStream)
  }

  override def getHeader(name: String): String = findHeader(name).orNull
}

class ServiceResponse(httpServletResponse: HttpServletResponse) extends HttpServletResponseWrapper(httpServletResponse) {
  //Enable in case server header is wanted

  //final val serverName: String = "ubirch-trust-service/1.0"
  //setHeader("Server", serverName)
}

/**
  * Represents a Handler that creates the customized request.
  * It should be mixed it with the corresponding ScalatraServlet.
  */
trait RequestEnricher extends Handler {
  abstract override def handle(request: HttpServletRequest, res: HttpServletResponse): Unit = {
    super.handle(new ServiceRequest(request), new ServiceResponse(res))
  }
}

/**
  * Represents the base for a controllers that supports the ServiceRequest
  * and adds helpers to handle async responses and body parsing and extraction.
  */
abstract class ControllerBase extends ScalatraServlet
  with RequestEnricher
  with FutureSupport
  with NativeJsonSupport
  with SwaggerSupport
  with CorsSupport
  with ServiceMetrics
  with LazyLogging {

  def asyncResult(name: String)(body: HttpServletRequest => HttpServletResponse => Task[ActionResult])(implicit request: HttpServletRequest, response: HttpServletResponse, scheduler: Scheduler): AsyncResult = {
    asyncResultCore {
      () =>
        count(name) {
          (for {
            fiber <- actionResult(body).start
            res <- fiber.join
          } yield res).runToFuture
        }
    }
  }

  private def asyncResultCore(body: () => CancelableFuture[ActionResult]): AsyncResult = {
    new AsyncResult() { override val is = body() }
  }

  private def actionResult(body: HttpServletRequest => HttpServletResponse => Task[ActionResult])(implicit request: HttpServletRequest, response: HttpServletResponse): Task[ActionResult] = {
    for {
      _ <- Task.delay(logRequestInfo)
      res <- Task.defer(body(request)(response))
        .onErrorHandle {
          case FailedExtractionException(_, rawBody, e) =>
            val msg = s"Couldn't parse [$rawBody] due to exception=${e.getClass.getCanonicalName} message=${e.getMessage}"
            logger.error(msg)
            BadRequest(NOK.parsingError(msg))
          case e: Exception =>
            val cause = Try(e.getCause.getMessage).getOrElse(e.getMessage)
            logger.error(s"Error 0.1 exception=${e.getClass.getCanonicalName} message=$cause", e)
            InternalServerError(NOK.serverError("Sorry, something happened"))
        }
    } yield {
      res
    }

  }

  private def logRequestInfo(implicit request: HttpServletRequest): Unit = {
    val path = request.getRequestURI
    val method = request.getMethod
    //val headers = request.headers.toList.map { case (k, v) => k + ":" + v }.mkString(",")
    //logger.info("Path[{}]:{} {}", method, path, headers)
    logger.debug("Path[{}]:{}", method, path)
  }

  case class ReadBody[T](extracted: T, asString: String)
  case class FailedExtractionException(message: String, body: String, throwable: Throwable) extends Exception(message, throwable) with NoStackTrace

  object ReadBody {

    def getBodyAsBytes(implicit request: HttpServletRequest): Try[(Array[Byte], String)] = for {
      bytes <- Try(IOUtils.toByteArray(request.getInputStream))
      bytesAsString <- Try(new String(bytes, StandardCharsets.UTF_8))
    } yield (bytes, bytesAsString)

    def getBodyAsString(implicit request: HttpServletRequest): Try[String] = Try(request.body)

    def store(bytes: Array[Byte]): Unit = {
      val date = new Date()
      val os = new FileOutputStream(s"src/main/scala/com/ubirch/curl/data_${date.getTime}.mpack")
      os.write(bytes)
      os.close()
    }

    def readJson[T: Manifest](transformF: JValue => JValue)(implicit request: HttpServletRequest): ReadBody[T] = {
      lazy val _body = getBodyAsString
      val parsed = for {
        body <- _body
        b <- Try(transformF(parse(body)).extract[T])
      } yield ReadBody(b, body)

      parsed.recover { case e => throw FailedExtractionException("Error Parsing: " + e.getMessage, "---", e) }.get
    }

    /**
      * This methods parse a request body with the charset because the body method of HttpServletRequest is using UTF-8 for only application/json.
      */
    def readBodyWithCharset(request: HttpServletRequest, charset: Charset): Task[String] =
      Task(Source.fromInputStream(request.getInputStream, charset.name()).mkString)

  }

}
