package com.ubirch

import scala.util.control.NoStackTrace

abstract class ServiceException(message: String) extends Exception(message) with NoStackTrace {
  val name: String = this.getClass.getCanonicalName
  def reason: String
}

case class InvalidOtherClaims(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value

}
case class InvalidAllClaims(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value

}
case class InvalidSpecificClaim(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value

}
