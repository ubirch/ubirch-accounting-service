package com.ubirch.models

import io.getquill.MappedEncoding
import org.json4s.JsonAST.JValue

trait JsonEncoding {

  def stringifyJValue(value: JValue): String
  def toJValue(value: String): JValue

  implicit def encodeJValue: MappedEncoding[JValue, String] = {
    MappedEncoding[JValue, String](Option(_).map(stringifyJValue).getOrElse(""))
  }
  implicit def decodeJValue: MappedEncoding[String, JValue] = {
    MappedEncoding[String, JValue](x => toJValue(x))
  }
}
