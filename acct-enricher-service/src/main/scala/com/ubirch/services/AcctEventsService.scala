package com.ubirch.services

import com.ubirch.models.cassandra.{ AcctEventOwnerRow, AcctEventRow, AcctStoreDAO }

import monix.eval.Task
import monix.reactive.Observable

import java.time.LocalDate
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def monthCount(
      identityId: UUID,
      tenantId: UUID,
      category: String,
      date: LocalDate,
      subCategory: Option[String]
  ): Task[MonthlyCountResult]

  def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow]

  def byTime(
      identityId: UUID,
      category: String,
      date: LocalDate,
      subCategory: Option[String]
  ): Observable[AcctEventRow]
}

case class MonthlyCountResult(identityId: UUID, tenantId: UUID, category: String, date: LocalDate, subCategory: Option[String], year: Int, month: Int, count: Long)

@Singleton
class DefaultAcctEventsService @Inject() (acctStoreDAO: AcctStoreDAO) extends AcctEventsService {

  def monthRange(date: LocalDate): Seq[(Int, Int)] = (1 to date.lengthOfMonth).flatMap { day =>
    (0 to 23).map { hour =>
      (day, hour)
    }
  }

  override def monthCount(identityId: UUID, tenantId: UUID, category: String, date: LocalDate, subCategory: Option[String]): Task[MonthlyCountResult] = {

    val tasks = monthRange(date).par.map { case (day, hour) =>
      acctStoreDAO.events.count(
        identityId = identityId,
        category = category,
        year = date.getYear,
        month = date.getMonthValue,
        day = day,
        hour = hour,
        subCategory = subCategory
      ).foldLeftL(0L)((a, b) => a + b)
    }.toList

    Task
      .parSequenceUnordered(tasks)
      .map { count =>
        MonthlyCountResult(
          identityId = identityId,
          tenantId = tenantId,
          category = category,
          date = date,
          subCategory = subCategory,
          year = date.getYear,
          month = date.getMonthValue,
          count = count.sum
        )
      }

  }

  override def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow] = {
    acctStoreDAO.owner.byOwnerId(ownerId)
  }

  override def byTime(identityId: UUID, category: String, date: LocalDate, subCategory: Option[String]): Observable[AcctEventRow] = {

    Observable.fromIterable(monthRange(date)).flatMap { case (day, hour) =>
      acctStoreDAO.events.byTime(
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
}
