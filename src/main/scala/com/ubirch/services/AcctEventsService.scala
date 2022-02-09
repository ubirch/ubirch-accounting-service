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

  override def count(identityId: UUID, category: String, date: LocalDate, subCategory: Option[String]): Observable[MonthlyCountResult] = {

    acctStoreDAO.events.count(
      identityId,
      category,
      date.getYear,
      date.getMonthValue,
      (1 to date.lengthOfMonth).toList,
      (0 to 23).toList,
      subCategory
    ).map(count => MonthlyCountResult(date.getYear, date.getMonthValue, count))

  }

  override def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow] = {
    acctStoreDAO.owner.byOwnerId(ownerId)
  }

}
