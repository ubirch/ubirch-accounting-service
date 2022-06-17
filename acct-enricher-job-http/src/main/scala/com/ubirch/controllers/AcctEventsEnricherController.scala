package com.ubirch.controllers

import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.ServiceException
import com.ubirch.controllers.concerns.ControllerBase
import com.ubirch.models.{ NOK, Return }
import com.ubirch.util.TaskHelpers

import com.typesafe.config.Config
import io.prometheus.client.Counter
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class AcctEventsEnricherController @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with ContentEncodingSupport with TaskHelpers {

  override protected val applicationDescription = "Acct Events Controller"
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

    asyncResult("acct_events_summary") { _ => _ =>
      (for {

        _ <- Task.unit

      } yield {
        Ok(Return("done"))
      }).onErrorHandle {
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

