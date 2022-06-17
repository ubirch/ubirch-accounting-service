package com.ubirch.models.postgres

import io.getquill.MappedEncoding

trait MapEncoding {

  def stringifyMap(value: Map[String, String]): String

  def toMap(value: String): Map[String, String]

  implicit def encodeJValue: MappedEncoding[Map[String, String], String] = {
    MappedEncoding[Map[String, String], String](Option(_).map(stringifyMap).getOrElse(""))
  }

  implicit def decodeJValue: MappedEncoding[String, Map[String, String]] = {
    MappedEncoding[String, Map[String, String]](toMap)
  }
}
