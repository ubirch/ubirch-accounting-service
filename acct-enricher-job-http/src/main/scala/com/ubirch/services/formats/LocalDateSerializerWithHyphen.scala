package com.ubirch.services.formats

import com.ubirch.util.DateUtil

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateSerializerWithHyphen extends CustomSerializer[LocalDate](_ => ({
  case JString(s) =>
    DateUtil.dateToLocalDate(DateUtil.`yyyy-MM-dd_NotLenient`.parse(s))
},
  {
    case d: LocalDate =>
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      JString(formatter.format(d))
  }))
