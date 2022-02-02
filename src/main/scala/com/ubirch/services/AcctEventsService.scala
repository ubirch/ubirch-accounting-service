package com.ubirch.services

import com.ubirch.models.{ AcctEventDAO, AcctEventRow }

import monix.reactive.Observable

import java.time.LocalDate
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def byOwnerIdAndIdentityId(
      identityId: UUID,
      category: String,
      date: LocalDate,
      hour: Int,
      subCategory: Option[String]
  ): Observable[AcctEventRow]

  def count(
      identityId: UUID,
      category: String,
      date: LocalDate,
      hour: Int,
      subCategory: Option[String]
  ): Observable[Long]
}

@Singleton
class DefaultAcctEventsService @Inject() (acctEventDAO: AcctEventDAO) extends AcctEventsService {

  override def byOwnerIdAndIdentityId(
      identityId: UUID,
      category: String,
      date: LocalDate,
      hour: Int,
      subCategory: Option[String]
  ): Observable[AcctEventRow] = {
    acctEventDAO.byOwnerIdAndIdentityId(
      identityId,
      category,
      date.getYear,
      date.getMonthValue,
      date.getDayOfMonth,
      hour,
      subCategory
    )
  }

  override def count(identityId: UUID, category: String, date: LocalDate, hour: Int, subCategory: Option[String]): Observable[Long] = {
    acctEventDAO.count(
      identityId,
      category,
      date.getYear,
      date.getMonthValue,
      date.getDayOfMonth,
      hour,
      subCategory
    )
  }

}
