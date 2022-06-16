package com.ubirch.services

import com.ubirch.models.cassandra.AcctStoreDAO

import monix.eval.Task

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
}

case class MonthlyCountResult(identityId: UUID, tenantId: UUID, category: String, date: LocalDate, subCategory: Option[String], year: Int, month: Int, count: Long)

@Singleton
class DefaultAcctEventsService @Inject() (acctStoreDAO: AcctStoreDAO) extends AcctEventsService {

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

  def monthRange(date: LocalDate): Seq[(Int, Int)] = (1 to date.lengthOfMonth).flatMap { day =>
    (0 to 23).map { hour =>
      (day, hour)
    }
  }

}
