package com.ubirch.services.externals

case class ResponseData[T](status: Int, headers: Map[String, List[String]], body: T)
