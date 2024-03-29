package com.ubirch.controllers

import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.{ NotFoundException, ServiceException }
import com.ubirch.api.InvalidClaimException
import com.ubirch.controllers.concerns.{ BearerAuthStrategy, ControllerBase }
import com.ubirch.defaults.TokenApi
import com.ubirch.models.{ NOK, Return }
import com.ubirch.services.SummaryService
import com.ubirch.util.{ DateUtil, TaskHelpers }

import com.typesafe.config.Config
import io.prometheus.client.Counter
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class AcctEventsEnricherController @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats,
    summaryService: SummaryService
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with ContentEncodingSupport with TaskHelpers {

  override protected val applicationDescription = "Acct Events Enricher Controller"
  override protected implicit def jsonFormats: Formats = jFormats

  override val service: String = config.getString(GenericConfPaths.NAME)

  override val successCounter: Counter = Counter.build()
    .name("acct_events_enricher_success")
    .help("Represents the number of acct events successes")
    .labelNames("service", "method")
    .register()

  override val errorCounter: Counter = Counter.build()
    .name("acct_events_enricher_failures")
    .help("Represents the number of acct events failures")
    .labelNames("service", "method")
    .register()

  before() {
    contentType = "application/json"
  }

  get("/v1") {

    asyncResult("acct_events_summary") { implicit request => _ =>
      (for {

        _ <- Task.unit

        claims <- Task.fromTry(TokenApi.decodeAndVerify(BearerAuthStrategy.request2BearerAuthRequest(request).token))
          .onErrorRecoverWith { case e: Exception => Task.raiseError(InvalidClaimException("Error authenticating", e.getMessage)) }
        _ <- Task.fromTry(claims.validateScope("thing:getinfo"))

        tenantIdRaw <- Task(params.get("tenant_id"))
        tenantId <- Task(tenantIdRaw)
          .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid Tenant Id: wrong tenant_id param: " + tenantIdRaw.getOrElse("")))

        orderRef <- Task(params.get("order_ref"))
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()).get)
          .onErrorHandle(_ => throw new IllegalArgumentException("Order ref: wrong order_ref param"))

        cat <- Task(params.get("cat"))
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))

        invoiceIdRaw <- Task(params.get("invoice_id"))
        invoiceId <- Task(invoiceIdRaw)
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()).get)
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid Invoice Id: wrong invoice id param: " + invoiceIdRaw.getOrElse("")))

        invoiceDate <- Task(params.get("invoice_date"))
          .map(_.map(DateUtil.`yyyy-MM-dd_NotLenient`.parse))
          .map(_.map(x => DateUtil.dateToLocalDate(x)).get)
          .onErrorHandle { e =>
            throw new IllegalArgumentException("Invalid Invoice Date: Use yyyy-MM-dd this format: " + e.getMessage)
          }

        from <- Task(params.get("from"))
          .map(_.map(DateUtil.`yyyy-MM-dd_NotLenient`.parse))
          .map(_.map(x => DateUtil.dateToLocalDate(x)))
          .onErrorHandle { e =>
            throw new IllegalArgumentException("Invalid From: Use yyyy-MM-dd this format: " + e.getMessage)
          }

        to <- Task(params.get("to"))
          .map(_.map(DateUtil.`yyyy-MM-dd_NotLenient`.parse))
          .map(_.map(x => DateUtil.dateToLocalDate(x)))
          .onErrorHandle { e =>
            throw new IllegalArgumentException("Invalid To: Use yyyy-MM-dd this format: " + e.getMessage)
          }

        _ <- earlyResponseIf(from.isDefined && to.isEmpty)(new IllegalArgumentException("Invalid Range Definition: Start requires End"))
        _ <- earlyResponseIf(from.isEmpty && to.isDefined)(new IllegalArgumentException("Invalid Range Definition: End requires Start"))
        _ <- earlyResponseIf({
          for {
            f <- from
            t <- to
          } yield f.isAfter(t)
        }.getOrElse(false))(new IllegalArgumentException("Invalid Range Definition: From must be before To"))

        res <- summaryService.get(invoiceId = invoiceId, invoiceDate = invoiceDate, from = from.get, to = to.get, orderRef = orderRef, tenantId, cat)

        _ = logger.info(s"query: tenant_id->$tenantId, order_ref->$orderRef, cat=${cat.getOrElse("")}, invoice_id->$invoiceId, invoice_date=${invoiceDate}, from=${from.getOrElse("")}, to=${to.getOrElse("")}")

      } yield {
        Ok(Return(res))
      }).onErrorHandle {
        case e: InvalidClaimException =>
          logger.error("1.0 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.value)
          Forbidden(NOK.authenticationError("Forbidden"))
        case e: NotFoundException =>
          logger.error("1.1 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          NotFound(NOK.notFound(e.getMessage))
        case e: ServiceException =>
          logger.error("1.2 Error getting acct identity by owner: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          BadRequest(NOK.acctEventQueryError(s"Error acct identity by owner. ${e.getMessage}"))
        case e: IllegalArgumentException =>
          logger.error("1.3 Error getting acct identity by owner: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          BadRequest(NOK.acctEventQueryError(s"Sorry, there is something invalid in your request: ${e.getMessage}"))
        case e: Exception =>
          logger.error(s"1.4 Error acct identity by owner: exception=${e.getClass.getCanonicalName} message=${e.getMessage}", e)
          InternalServerError(NOK.serverError("Sorry, something went wrong on our end"))
      }
    }
  }

  notFound {
    asyncResult("not_found") { _ => _ =>
      Task {
        logger.info("controller=AcctEventsEnricherController route_not_found={} query_string={}", requestPath, Option(request).map(_.getQueryString).getOrElse(""))
        NotFound(NOK.noRouteFound(requestPath + " might exist in another universe"))
      }
    }
  }

  def swaggerTokenAsHeader: SwaggerSupportSyntax.ParameterBuilder[String] = headerParam[String]("Authorization")
    .description("Ubirch Token. ADD \"bearer \" followed by a space) BEFORE THE TOKEN OTHERWISE IT WON'T WORK")

}

