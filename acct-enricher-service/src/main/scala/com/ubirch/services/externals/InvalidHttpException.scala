package com.ubirch.services.externals

import com.ubirch.ServiceException

case class InvalidHttpException(message: String, value: String) extends ServiceException(message) {
  override def reason: String = value
}

case class HttpResponseException(
    targetSystem: Symbol,
    message: String,
    statusCode: Int,
    headers: Map[String, List[String]],
    body: String
) extends ServiceException(message) {
  override def reason: String = message

}

