package com.ubirch.controllers

import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ ControllerBase, KeycloakBearerAuthStrategy, KeycloakBearerAuthenticationSupport, SwaggerElements }
import com.ubirch.models.{ AcctEventRow, NOK, Return }
import com.ubirch.services.AcctEventsService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenVerificationService }
import com.ubirch.util.{ DateUtil, TaskHelpers }
import com.ubirch.{ InvalidSecurityCheck, ServiceException }

import com.typesafe.config.Config
import io.prometheus.client.Counter
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class AcctEventsController @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats,
    acctEvents: AcctEventsService,
    publicKeyPoolService: PublicKeyPoolService,
    tokenVerificationService: TokenVerificationService
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with KeycloakBearerAuthenticationSupport with TaskHelpers {

  override protected val applicationDescription = "Acct Events Controller"
  override protected implicit def jsonFormats: Formats = jFormats

  override val service: String = config.getString(GenericConfPaths.NAME)

  override val successCounter: Counter = Counter.build()
    .name("acct_events_success")
    .help("Represents the number of acct events successes")
    .labelNames("service", "method")
    .register()

  override val errorCounter: Counter = Counter.build()
    .name("acct_events_failures")
    .help("Represents the number of acct events failures")
    .labelNames("service", "method")
    .register()

  before() {
    contentType = "application/json"
  }

  val getV1: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[List[AcctEventRow]]("getV1TokenList")
      summary "Queries for the accounting events for a logged in user."
      description "Queries for the accounting events for a logged in user. You can specify the target identity."
      tags SwaggerElements.TAG_SERVICE
      parameters (
        swaggerTokenAsHeader,
        pathParam[String]("ownerId").description("The uuid for the owner. It could be the user.").required,
        queryParam[String]("identity_id").optional.description("The uuid that belongs to the identity or device")
      ))

  get("/v1/:owner_id", operation(getV1)) {

    lazy val sdf = new SimpleDateFormat("yyyy-MM-dd")

    authenticated() { token =>

      asyncResult("list_acct_events_owner") { implicit request => _ =>
        (for {

          //mandatory -start
          identityId <- Task(params.get("identity_id"))
            .map(_.map(UUID.fromString).getOrElse(throw new IllegalArgumentException("Invalid Identity Id")))
            .onErrorHandle(_ => throw new IllegalArgumentException("Invalid identity_id: wrong identity_id param"))

          cat <- Task(params.get("cat"))
            .map(_.filter(_.nonEmpty))
            .map(_.map(_.toLowerCase()).getOrElse(throw new IllegalArgumentException("Invalid category Definition: End requires category")))
            .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))

          date <- Task(params.get("date"))
            .map(_.map(sdf.parse))
            .map(_.map(x => DateUtil.dateToLocalDate(x, ZoneId.systemDefault())).getOrElse(throw new IllegalArgumentException("Invalid Date Definition: End requires Date")))
            .onErrorHandle(_ => throw new IllegalArgumentException("Invalid Date: Use yyyy-MM-dd this format"))

          hour <- Task(params.get("hour"))
            .map(_.map(_.toInt).getOrElse(throw new IllegalArgumentException("Invalid Hour Definition: 0-23 format")))
            .onErrorHandle(_ => throw new IllegalArgumentException("Invalid Hour: Use h this format"))

          //mandatory -end

          //optional -start
          subCat <- Task(params.get("sub_cat"))
            .map(_.filter(_.nonEmpty))
            .map(_.map(_.toLowerCase()))
            .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))

          rawOwnerId <- Task(params.get("owner_id"))
          ownerId <- Task(rawOwnerId)
            .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
            .onErrorHandle(_ => throw new IllegalArgumentException("Invalid OwnerId: wrong owner param: " + rawOwnerId.getOrElse("")))
          ownerIdFromToken <- Task.delay(token.ownerIdAsUUID.getOrElse(throw new IllegalArgumentException("Invalid Token OwnerI: Wrong token owner")))
          ownerCheck = (ownerIdFromToken == ownerId) || token.isAdmin
          _ <- earlyResponseIf(!ownerCheck)(InvalidSecurityCheck("Invalid Owner Relation", "You can't access somebody else's data"))
          //optional -end

          mode <- Task(params.get("mode")).map(_.map(_.trim).orElse(Some("events")))
          evs <- mode match {
            case Some("count") => acctEvents.count(identityId, cat, date, hour, subCat).toListL
            case Some("events") => acctEvents.byOwnerIdAndIdentityId(identityId, cat, date, hour, subCat).toListL
            case other => throw new IllegalArgumentException(s"Invalid mode: wrong mode param -> ${other.getOrElse("")}")
          }

          _ = logger.info(s"query: mode->${mode.getOrElse("")}, , owner_id->$ownerId, cat=$cat, identity_id->$identityId, date=$date, hour=$hour, sub_cat=$subCat")

        } yield {
          Ok(Return(evs))
        }).onErrorHandle {
          case e: InvalidSecurityCheck =>
            logger.error("1.0 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.reason)
            Forbidden(NOK.authenticationError("Forbidden"))
          case e: ServiceException =>
            logger.error("1.1 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
            BadRequest(NOK.acctEventQueryError(s"Error querying acct event. ${e.getMessage}"))
          case e: IllegalArgumentException =>
            logger.error("1.2 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
            BadRequest(NOK.acctEventQueryError(s"Sorry, there is something invalid in your request: ${e.getMessage}"))
          case e: Exception =>
            logger.error(s"1.3 Error querying acct event: exception=${e.getClass.getCanonicalName} message=${e.getMessage}", e)
            InternalServerError(NOK.serverError("Sorry, something went wrong on our end"))
        }
      }
    }
  }

  notFound {
    asyncResult("not_found") { _ => _ =>
      Task {
        logger.info("controller=AcctEventsController route_not_found={} query_string={}", requestPath, Option(request).map(_.getQueryString).getOrElse(""))
        NotFound(NOK.noRouteFound(requestPath + " might exist in another universe"))
      }
    }
  }

  override protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy = {
    new KeycloakBearerAuthStrategy(app, tokenVerificationService, publicKeyPoolService)
  }

  def swaggerTokenAsHeader: SwaggerSupportSyntax.ParameterBuilder[String] = headerParam[String]("Authorization")
    .description("Token of the user. ADD \"bearer \" followed by a space) BEFORE THE TOKEN OTHERWISE IT WON'T WORK")

}

