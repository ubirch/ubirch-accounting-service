package com.ubirch.controllers

import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.ServiceException
import com.ubirch.api.InvalidClaimException
import com.ubirch.controllers.concerns.{ BearerAuthStrategy, ControllerBase, SwaggerElements }
import com.ubirch.defaults.TokenApi
import com.ubirch.models.{ AcctEvent, AcctEventRow, NOK, Return }
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.{ AcctEventsService, AcctEventsStoreService }
import com.ubirch.util.{ DateUtil, TaskHelpers }

import com.typesafe.config.Config
import io.prometheus.client.Counter
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Consumer
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.time.{ LocalDate, ZoneId }
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class AcctEventsController @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats,
    acctEvents: AcctEventsService,
    acctEventsStore: AcctEventsStoreService,
    jsonConverterService: JsonConverterService
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with ContentEncodingSupport with TaskHelpers {

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
    (apiOperation[List[AcctEventRow]]("getV1")
      summary "Queries for the accounting events for an identity."
      description "Queries for the accounting events for an identity."
      tags SwaggerElements.TAG_SERVICE
      parameters (
        swaggerTokenAsHeader,
        queryParam[String]("identity_id").optional.description("The uuid that belongs to the identity or device"),
        queryParam[String]("cat").optional.description("Principal category"),
        queryParam[LocalDate]("date").optional.description("Date for the query. Use yyyy-MM-dd this format"),
        queryParam[Int]("hour").optional.description("Date for the query. Hour Definition: 0-23 format"),
        queryParam[Int]("sub_cat").optional.description("Subcategory for query").optional
      ))

  get("/v1/:identity_id", operation(getV1)) {

    lazy val sdf = new SimpleDateFormat("yyyy-MM")

    asyncResult("list_acct_events_identity") { implicit request => _ =>
      (for {

        _ <- Task.unit

        claims <- Task.fromTry(TokenApi.decodeAndVerify(BearerAuthStrategy.request2BearerAuthRequest(request).token))
          .onErrorRecoverWith { case e: Exception => Task.raiseError(InvalidClaimException("Error authenticating", e.getMessage)) }
        _ <- Task.fromTry(claims.validateScope("thing:getinfo"))

        //mandatory -start
        rawIdentityId <- Task(params.get("identity_id"))
        identityId <- Task(rawIdentityId)
          .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
          .flatMap(x => Task.fromTry(claims.validateIdentity(x)))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid identity_id: wrong identity param: " + rawIdentityId.getOrElse("")))

        cat <- Task(params.get("cat"))
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()).getOrElse(throw new IllegalArgumentException("Invalid category Definition: End requires category")))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))

        date <- Task(params.get("date"))
          .map(_.map(sdf.parse))
          .map(_.map(x => DateUtil.dateToLocalDate(x, ZoneId.systemDefault())).getOrElse(throw new IllegalArgumentException("Invalid Date Definition: End requires Date")))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid Date: Use yyyy-MM this format"))
        //mandatory -end

        //optional -start
        subCat <- Task(params.get("sub_cat"))
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))
        //optional -end

        evs <- acctEvents.monthCount(identityId, cat, date, subCat).map(x => List(x))

        _ = logger.info(s"query: cat=$cat, identity_id->$identityId, date=$date, sub_cat=$subCat")

      } yield {
        Ok(Return(evs))
      }).onErrorHandle {
        case e: InvalidClaimException =>
          logger.error("1.0 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.value)
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

  val getChunkedV1: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[List[AcctEventRow]]("getChunkedV1")
      summary "Queries for the accounting events for an identity. Result is chunked"
      description "Queries for the accounting events for an identity. Result is chunked"
      tags SwaggerElements.TAG_SERVICE
      parameters (
        swaggerTokenAsHeader,
        queryParam[String]("identity_id").optional.description("The uuid that belongs to the identity or device"),
        queryParam[String]("cat").optional.description("Principal category"),
        queryParam[LocalDate]("date").optional.description("Date for the query. Use yyyy-MM-dd this format"),
        queryParam[Int]("hour").optional.description("Date for the query. Hour Definition: 0-23 format"),
        queryParam[Int]("sub_cat").optional.description("Subcategory for query").optional
      ))

  get("/v1/chunked/:identity_id", operation(getChunkedV1)) {

    def getConsumer(output: PrintWriter) = {
      Consumer.foreach[AcctEventRow] { acctEvent =>
        output.write(jsonConverterService.toString[AcctEventRow](acctEvent).toTry.get + "\n")
        output.flush()
      }
    }

    lazy val sdf = new SimpleDateFormat("yyyy-MM")

    asyncResult("list_acct_events_identity", 60 seconds) { implicit request => response =>

      (for {

        claims <- Task.fromTry(TokenApi.decodeAndVerify(BearerAuthStrategy.request2BearerAuthRequest(request).token))
          .onErrorRecoverWith { case e: Exception => Task.raiseError(InvalidClaimException("Error authenticating", e.getMessage)) }
        _ <- Task.fromTry(claims.validateScope("thing:getinfo"))

        //mandatory -start
        rawIdentityId <- Task(params.get("identity_id"))
        identityId <- Task(rawIdentityId)
          .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
          .flatMap(x => Task.fromTry(claims.validateIdentity(x)))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid identity_id: wrong identity param: " + rawIdentityId.getOrElse("")))

        cat <- Task(params.get("cat"))
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()).getOrElse(throw new IllegalArgumentException("Invalid category Definition: End requires category")))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))

        date <- Task(params.get("date"))
          .map(_.map(sdf.parse))
          .map(_.map(x => DateUtil.dateToLocalDate(x, ZoneId.systemDefault())).getOrElse(throw new IllegalArgumentException("Invalid Date Definition: End requires Date")))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid Date: Use yyyy-MM this format"))
        //mandatory -end

        //optional -start
        subCat <- Task(params.get("sub_cat"))
          .map(_.filter(_.nonEmpty))
          .map(_.map(_.toLowerCase()))
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid cat: wrong cat param"))
        //optional -end

        //http chunks
        output <- Task.delay {
          response.setHeader("Content-Type", "application/json")
          response.setHeader("Transfer-Encoding", "chunked")
          response.setStatus(200)
          response.getWriter
        }

        _ <- acctEvents.byTime(identityId, cat, date, subCat)
          .doOnStart(_ => Task.delay(logger.info(s"streaming_started: cat=$cat, identity_id->$identityId, date=$date, sub_cat=$subCat")))
          .doOnComplete(Task.delay(logger.info(s"streaming_ended: cat=$cat, identity_id->$identityId, date=$date, sub_cat=$subCat")))
          .consumeWith(getConsumer(output))

      } yield {
        Ok(())
      }).onErrorHandle {
        case e: InvalidClaimException =>
          logger.error("1.0 Error querying acct event: exception={} message={}", e.getClass.getCanonicalName, e.value)
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

  val postStoreV1: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Return]("getPostStoreV1")
      consumes "application/json"
      produces "application/json"
      summary "Stores a list of events."
      description "Stores a list of events for accounting purposes"
      tags SwaggerElements.TAG_SERVICE
      parameters (
        swaggerTokenAsHeader,
        bodyParam[List[AcctEvent]]("AcctEvent")
      ))

  post("/v1/record", operation(postStoreV1)) {

    asyncResult("acct_events_store") { implicit request => _ =>
      (for {

        _ <- Task.unit

        claims <- Task.fromTry(TokenApi.decodeAndVerify(BearerAuthStrategy.request2BearerAuthRequest(request).token))
          .onErrorRecoverWith { case e: Exception => Task.raiseError(InvalidClaimException("Error authenticating", e.getMessage)) }

        _ <- Task.fromTry(claims.validateScope("thing:storedata"))

        data <- Task.delay(ReadBody.readJson[List[AcctEvent]](x => x.camelizeKeys))
          .onErrorRecoverWith {
            case e: Exception => Task.raiseError(new IllegalArgumentException(s"error parsing body. " + e.getMessage))
          }

        _ <- Task.delay(data.extracted).map { xs =>
          xs.map(x => claims.validateIdentity(x.identityId).get)
        }

        res <- acctEventsStore.store(data.extracted).map(_ => "accepted")

      } yield {
        Accepted(Return(res))
      }).onErrorHandle {
        case e: InvalidClaimException =>
          logger.error("1.0 Error storing acct event: exception={} message={}", e.getClass.getCanonicalName, e.value)
          Forbidden(NOK.authenticationError("Forbidden"))
        case e: ServiceException =>
          logger.error("1.1 Error storing acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          BadRequest(NOK.acctEventQueryError(s"Error storing acct event. ${e.getMessage}"))
        case e: IllegalArgumentException =>
          logger.error("1.2 Error storing acct event: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          BadRequest(NOK.acctEventQueryError(s"Sorry, there is something invalid in your request: ${e.getMessage}"))
        case e: Exception =>
          logger.error(s"1.3 Error storing acct event: exception=${e.getClass.getCanonicalName} message=${e.getMessage}", e)
          InternalServerError(NOK.serverError("Sorry, something went wrong on our end"))
      }
    }
  }

  val getKnownOwnersV1: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Return]("getPostStoreV1")
      consumes "application/json"
      produces "application/json"
      summary "Gets the known identities for an owner."
      description "Gets the known identities for an owner. Note that owners are optional in the registration of events. That's to say, that it is possible that " +
      "some owners be not found."
      tags SwaggerElements.TAG_SERVICE
      parameters swaggerTokenAsHeader)

  get("/v1/:owner_id/identities", operation(getKnownOwnersV1)) {

    asyncResult("acct_events_owner_store") { implicit request => _ =>
      (for {

        _ <- Task.unit

        claims <- Task.fromTry(TokenApi.decodeAndVerify(BearerAuthStrategy.request2BearerAuthRequest(request).token))
          .onErrorRecoverWith { case e: Exception => Task.raiseError(InvalidClaimException("Error authenticating", e.getMessage)) }

        _ <- Task.fromTry(claims.validateScope("thing:getinfo"))

        rawOwnerId <- Task(params.get("owner_id"))
        ownerId <- Task(rawOwnerId)
          .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
          .onErrorHandle(_ => throw new IllegalArgumentException("Invalid OwnerId: wrong owner param: " + rawOwnerId.getOrElse("")))

        ownerIdFromToken <- Task.delay(claims.isSubjectUUID.getOrElse(throw new IllegalArgumentException("Invalid Token OwnerI: Wrong token owner")))

        ownerCheck = ownerIdFromToken == ownerId
        _ <- earlyResponseIf(!ownerCheck)(InvalidClaimException("Invalid Owner Relation", "You can't access somebody else's data"))

        res <- acctEvents.getKnownIdentitiesByOwner(ownerId).toListL

      } yield {
        Ok(Return(res))
      }).onErrorHandle {
        case e: InvalidClaimException =>
          logger.error("1.0 Error getting acct identity by owner: exception={} message={}", e.getClass.getCanonicalName, e.value)
          Forbidden(NOK.authenticationError("Forbidden"))
        case e: ServiceException =>
          logger.error("1.1 Error getting acct identity by owner: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          BadRequest(NOK.acctEventQueryError(s"Error acct identity by owner. ${e.getMessage}"))
        case e: IllegalArgumentException =>
          logger.error("1.2 Error getting acct identity by owner: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          BadRequest(NOK.acctEventQueryError(s"Sorry, there is something invalid in your request: ${e.getMessage}"))
        case e: Exception =>
          logger.error(s"1.3 Error acct identity by owner: exception=${e.getClass.getCanonicalName} message=${e.getMessage}", e)
          InternalServerError(NOK.serverError("Sorry, something went wrong on our end"))
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

  def swaggerTokenAsHeader: SwaggerSupportSyntax.ParameterBuilder[String] = headerParam[String]("Authorization")
    .description("Ubirch Token. ADD \"bearer \" followed by a space) BEFORE THE TOKEN OTHERWISE IT WON'T WORK")

}

