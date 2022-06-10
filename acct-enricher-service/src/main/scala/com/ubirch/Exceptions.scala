package com.ubirch

import scala.util.control.NoStackTrace

abstract class ServiceException(message: String) extends Exception(message) with NoStackTrace {
  val name: String = this.getClass.getCanonicalName
  def reason: String
}

case class NoContactPointsException(message: String) extends ServiceException(message) {
  override def reason: String = message
}
case class NoKeyspaceException(message: String) extends ServiceException(message) {
  override def reason: String = message
}
case class InvalidConsistencyLevel(message: String) extends ServiceException(message) {
  override def reason: String = message
}
case class InvalidContactPointsException(message: String) extends ServiceException(message) {
  override def reason: String = message
}
case class StoringException(message: String, reason: String) extends ServiceException(message)
case class InvalidParamException(message: String, reason: String) extends ServiceException(message)

case class InvalidOtherClaims(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value
}
case class InvalidAllClaims(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value
}
case class InvalidSpecificClaim(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value
}
