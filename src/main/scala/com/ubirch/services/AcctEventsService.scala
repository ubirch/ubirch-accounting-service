package com.ubirch.services

import com.ubirch.models.{ AcctEventOwnerRow, AcctStoreDAO }

import monix.reactive.Observable

import java.time.LocalDate
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def count(
      identityId: UUID,
      category: String,
      date: LocalDate,
      subCategory: Option[String]
  ): Observable[MonthlyCountResult]

  def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow]
}

case class MonthlyCountResult(year: Int, month: Int, count: Long)

@Singleton
class DefaultAcctEventsService @Inject() (acctStoreDAO: AcctStoreDAO) extends AcctEventsService {

  final val hoursObservable = Observable.fromIterable(0 to 23)

  override def count(identityId: UUID, category: String, date: LocalDate, subCategory: Option[String]): Observable[MonthlyCountResult] = {
    Observable
      .fromIterable(1 to date.lengthOfMonth)
      .flatMap { day =>
        hoursObservable.flatMap { hour =>
          acctStoreDAO.events.count(
            identityId = identityId,
            category = category,
            year = date.getYear,
            month = date.getMonthValue,
            day = day,
            hour = hour,
            subCategory = subCategory
          )
        }
      }
      .sum
      .map(count => MonthlyCountResult(date.getYear, date.getMonthValue, count))
  }

  override def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow] = {
    acctStoreDAO.owner.byOwnerId(ownerId)
  }

}
