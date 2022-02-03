package com.ubirch

import monix.eval.Task

import javax.servlet.http.HttpServletRequest
import scala.util.Try

package object controllers {

  def getHeader(request: HttpServletRequest, key: String): Task[String] = Task.fromTry {
    Try { request.getHeader(key) }
      .filter(x => Option(x).isDefined)
      .filter(_.nonEmpty)
      .recover {
        case _: Exception => throw new IllegalArgumentException(s"Header is missing ($key)")
      }
  }

}
