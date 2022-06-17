package com.ubirch.services.formats

import com.ubirch.util.DateUtil

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateSerializerWithHyphen extends CustomSerializer[LocalDate](_ => ({
  case JString(s) =>
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    DateUtil.dateToLocalDate(sdf.parse(s))
},
  {
    case d: LocalDate =>
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      JString(formatter.format(d))
  }))
