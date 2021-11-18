package com.ubirch.services.formats

import com.ubirch.util.DateUtil

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import java.text.SimpleDateFormat
import java.time.LocalDate

class LocalDateSerializer extends CustomSerializer[LocalDate](_ => ({
  case JString(s) =>
    val sdf = new SimpleDateFormat("yyyy-M-dd")
    DateUtil.dateToLocalTime(sdf.parse(s))
},
  {
    case d: LocalDate => JString(d.toString)
  }))

