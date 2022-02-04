package com.ubirch.services

import com.ubirch.models.AcctEventDAO

import monix.reactive.Observable

import java.time.{ Instant, LocalDate, ZoneId }
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def count(
      identityId: UUID,
      category: String,
      date: LocalDate,
      hour: Int,
      subCategory: Option[String]
  ): Observable[HourCountResult]
}

case class HourCountResult(year: Int, month: Int, day: Int, hour: Int, count: Long)

@Singleton
class DefaultAcctEventsService @Inject() (acctEventDAO: AcctEventDAO) extends AcctEventsService {

  final val normalHours = 0 to 23
  final val allHoursUntilNow = List(-1)
  final val allHours = List(-2)
  final val validHours = allHours ++ allHoursUntilNow ++ normalHours

  final lazy val invalidHourException =
    new IllegalArgumentException("Hour is not valid. Hour value should be one of these values (" + normalHours.mkString(",") + ") or "
      + allHoursUntilNow.mkString(",") + " to indicate all hours until now or "
      + allHours.mkString(",") + " to indicate all hours")

  override def count(identityId: UUID, category: String, date: LocalDate, hour: Int, subCategory: Option[String]): Observable[HourCountResult] = {

    def  doRange(range: Range) = {
      range
        .map(hour => count(identityId, category, date, hour, subCategory))
        .fold(Observable.empty[HourCountResult])((a, b) => a ++ b)
    }

    if (validHours.contains(hour)) {
      hour match {
        case -1 =>
          val currentHour = Instant.now().atZone(ZoneId.systemDefault()).toLocalTime.getHour
          doRange(0 to currentHour)
        case -2 =>
          doRange(normalHours)
        case hour =>
          acctEventDAO.count(
            identityId,
            category,
            date.getYear,
            date.getMonthValue,
            date.getDayOfMonth,
            hour,
            subCategory
          ).map(count => HourCountResult(date.getYear, date.getMonthValue, date.getDayOfMonth, hour, count))
      }
    } else {
      Observable.raiseError(invalidHourException)
    }

  }

}
