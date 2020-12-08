package com.ubirch.controllers

import java.util.UUID

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ ControllerBase, KeycloakBearerAuthStrategy, KeycloakBearerAuthenticationSupport }
import com.ubirch.models.{ Good, NOK }
import com.ubirch.services.AcctEventsService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenVerificationService }
import com.ubirch.util.TaskHelpers
import com.ubirch.{ InvalidParamException, InvalidSecurityCheck, ServiceException }
import io.prometheus.client.Counter
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.Swagger

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

  val service: String = config.getString(GenericConfPaths.NAME)

  val successCounter: Counter = Counter.build()
    .name("acct_events_success")
    .help("Represents the number of acct events successes")
    .labelNames("service", "method")
    .register()

  val errorCounter: Counter = Counter.build()
    .name("acct_events_failures")
    .help("Represents the number of acct events failures")
    .labelNames("service", "method")
    .register()

  before() {
    contentType = "application/json"
  }

  get("/v1/:ownerId") {

    authenticated() { token =>

      asyncResult("list_acct_events_owner") { _ =>
        (for {

          ownerId <- Task(params.getOrElse("ownerId", throw InvalidParamException("Invalid OwnerId", "No OwnerId parameter found in path")))
            .map(UUID.fromString)
            .onErrorHandle(_ => throw InvalidParamException("Invalid OwnerId", "Wrong owner param"))

          ownerCheck = token.ownerIdAsUUID.map(_ == ownerId).isSuccess || (token.ownerIdAsUUID.map(_ != ownerId).isSuccess && token.isAdmin)

          _ = earlyResponseIf(ownerCheck)(InvalidSecurityCheck("Invalid Owner Relation", "You can't access somebody else's data"))

          identityId <- Task(params.get("identity_id"))
            .map(_.map(UUID.fromString))
            .onErrorHandle(_ => throw InvalidParamException("Invalid identity_id", "Wrong identity_id param"))

          evs <- acctEvents.byOwnerIdAndIdentityId(ownerId, identityId)
            .toListL

        } yield {
          Ok(Good(evs))
        }).onErrorHandle {
          case e: InvalidSecurityCheck =>
            logger.error("1.0 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
            Forbidden(NOK.authenticationError("Forbidden"))
          case e: ServiceException =>
            logger.error("1.1 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
            BadRequest(NOK.acctEventQueryError("Error querying acct event"))
          case e: Exception =>
            logger.error(s"1.2 Error querying acct event: exception=${e.getClass.getCanonicalName} message=${e.getMessage}", e)
            InternalServerError(NOK.serverError("1.2 Sorry, something went wrong on our end"))
        }
      }
    }
  }

  notFound {
    asyncResult("not_found") { _ =>
      Task {
        logger.info("controller=AcctEventsController route_not_found={} query_string={}", requestPath, request.getQueryString)
        NotFound(NOK.noRouteFound(requestPath + " might exist in another universe"))
      }
    }
  }

  override protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy = {
    new KeycloakBearerAuthStrategy(app, tokenVerificationService, publicKeyPoolService)
  }

}

