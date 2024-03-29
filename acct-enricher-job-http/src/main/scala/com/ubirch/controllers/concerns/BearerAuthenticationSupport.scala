package com.ubirch.controllers.concerns

import com.ubirch.models.NOK

import org.json4s.JNothing
import org.json4s.JsonAST.JValue
import org.scalatra.ScalatraBase
import org.scalatra.auth.{ ScentryConfig, ScentryStrategy, ScentrySupport }

import java.util.{ Locale, UUID }
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import scala.language.implicitConversions
import scala.util.Try

trait BearerAuthSupport[TokenType <: AnyRef] {
  self: ScalatraBase with ScentrySupport[TokenType] =>

  def realm: String

  protected def bearerAuth()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[TokenType] = {
    val beReq = new BearerAuthStrategy.BearerAuthRequest(request)
    if (!beReq.providesAuth) {
      val bearerRealm = "Bearer realm=\"%s\"" format realm
      halt(401, NOK.authenticationError("Unauthenticated"), Map("WWW-Authenticate" -> bearerRealm, "Content-Type" -> "application/json"))
    }
    if (!beReq.isBearerAuth) {
      halt(400, NOK.authenticationError("Invalid bearer token"), Map("Content-Type" -> "application/json"))
    }

    scentry.authenticate("Bearer")

  }

  protected def authenticated(p: (TokenType => Boolean)*)(action: TokenType => Any)(implicit request: HttpServletRequest, response: HttpServletResponse): Any = {
    val predicates: Seq[TokenType => Boolean] = p
    bearerAuth() match {
      case Some(value) if predicates.forall(x => x(value)) => action(value)
      case _ => halt(403, NOK.authenticationError("Forbidden"))
    }
  }

  protected def getToken(p: (TokenType => Boolean)*)(implicit request: HttpServletRequest, response: HttpServletResponse): Try[TokenType] = Try {
    val predicates: Seq[TokenType => Boolean] = p
    bearerAuth() match {
      case Some(value) if predicates.forall(x => x(value)) => value
      case _ => halt(403, NOK.authenticationError("Forbidden"))
    }
  }

}

object BearerAuthStrategy {

  implicit def request2BearerAuthRequest(r: HttpServletRequest): BearerAuthRequest = new BearerAuthRequest(r)

  private val AUTHORIZATION_KEYS = List("Authorization", "HTTP_AUTHORIZATION", "X-HTTP_AUTHORIZATION", "X_HTTP_AUTHORIZATION")

  class BearerAuthRequest(r: HttpServletRequest) {

    def parts: Seq[String] = authorizationKey map { r.getHeader(_).split(" ", 2).toList } getOrElse Nil
    def scheme: Option[String] = parts.headOption.map(sch => sch.toLowerCase(Locale.ENGLISH))
    def params: Option[String] = parts.lastOption

    private def authorizationKey: Option[String] = AUTHORIZATION_KEYS.find(r.getHeader(_) != null)

    def isBearerAuth: Boolean = scheme.foldLeft(false) { (_, sch) => sch == "bearer" }
    def providesAuth: Boolean = authorizationKey.isDefined

    private[this] val credentials = params.getOrElse("")
    def token: String = credentials
  }

}

abstract class BearerAuthStrategy[TokenType <: AnyRef](protected override val app: ScalatraBase) extends ScentryStrategy[TokenType] {

  import BearerAuthStrategy.request2BearerAuthRequest

  override def isValid(implicit request: HttpServletRequest): Boolean = request.isBearerAuth && request.providesAuth

  override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[TokenType] = {
    validate(request.token)
  }

  protected def validate(token: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[TokenType]

}

case class Token(value: String, json: JValue, sub: String, name: String, email: String, roles: List[Symbol]) {
  def id: String = sub
  def ownerId: String = id
  def isAdmin: Boolean = roles.contains(Token.ADMIN)
  def isUser: Boolean = roles.contains(Token.USER)
  def hasRole(role: Symbol): Boolean = roles.contains(role)
  def getRole(role: Symbol): Option[Symbol] = roles.find(_ == role)
  def ownerIdAsUUID: Try[UUID] = Try(UUID.fromString(ownerId))
}

object Token {
  final val ADMIN = Symbol("ADMIN")
  final val USER = Symbol("USER")
  def apply(value: String): Token = new Token(value, JNothing, sub = "", name = "", email = "", roles = Nil)
}

trait BearerAuthenticationSupport extends ScentrySupport[Token] with BearerAuthSupport[Token] {

  self: ScalatraBase =>

  override protected def fromSession: PartialFunction[String, Token] = {
    case a => Token(a)
  }

  override protected def toSession: PartialFunction[Token, String] = {
    case a => a.value
  }

  override protected val scentryConfig: ScentryConfiguration = {
    new ScentryConfig {}.asInstanceOf[ScentryConfiguration]
  }

  override def realm: String = "Ubirch Accounting Service"

}

