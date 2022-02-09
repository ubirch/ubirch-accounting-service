package com.ubirch.services

import com.ubirch.models.{ AcctEventOwnerRow, AcctStoreDAO }

import monix.eval.Task
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
  ): Task[MonthlyCountResult]

  def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow]
}

case class MonthlyCountResult(year: Int, month: Int, count: Long)

@Singleton
class DefaultAcctEventsService @Inject() (acctStoreDAO: AcctStoreDAO) extends AcctEventsService {

  def monthRange(date: LocalDate): Seq[(Int, Int)] = (1 to date.lengthOfMonth).flatMap { day =>
    (0 to 23).map { hour =>
      (day, hour)
    }
  }

  override def count(identityId: UUID, category: String, date: LocalDate, subCategory: Option[String]): Task[MonthlyCountResult] = {

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
      .map(count => MonthlyCountResult(date.getYear, date.getMonthValue, count.sum))

  }

  override def getKnownIdentitiesByOwner(ownerId: UUID): Observable[AcctEventOwnerRow] = {
    acctStoreDAO.owner.byOwnerId(ownerId)
  }

}
