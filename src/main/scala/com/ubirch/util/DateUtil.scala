package com.ubirch.util

import java.util.{ Calendar, Date }

import org.joda.time.format.{ DateTimeFormatter, ISODateTimeFormat }
import org.joda.time.{ DateTime, DateTimeZone, LocalTime, Period }

/**
  * Convenience for Dates
  */
object DateUtil {

  def nowUTC: DateTime = DateTime.now(DateTimeZone.UTC)

  def yesterdayUTC: DateTime = nowUTC.minusDays(1)

  def todayAtMidnight: DateTime = nowUTC.withTime(LocalTime.MIDNIGHT)

  def parseDateToUTC(dateString: String): DateTime = {

    ISODateTimeFormat.dateTime()
      .parseDateTime(dateString + "T00:00:00.000Z")
      .withZone(DateTimeZone.UTC)

  }

  def ISOFormatter: DateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC()

  def dateRange(from: DateTime, to: DateTime, stepSize: Period): Seq[DateTime] = {

    val now = nowUTC
    if (now.isAfter(now.plus(stepSize))) {

      Seq.empty

    } else {

      if (from.isBefore(to)) {
        Iterator.iterate(from)(_.plus(stepSize)).takeWhile(!_.isAfter(to)).toSeq
      } else {
        Iterator.iterate(from)(_.minus(stepSize)).takeWhile(!_.isBefore(to)).toSeq
      }

    }

  }

  def toString_YYYY_MM_dd(date: DateTime): String = date.toString("YYYY-MM-dd")

  def resetTimeInDate(date: Date): Date = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.HOUR, 0)
    calendar.getTime
  }

}
