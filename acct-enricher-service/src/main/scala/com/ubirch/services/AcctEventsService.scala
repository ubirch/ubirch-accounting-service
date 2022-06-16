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

  def dailyCount(
      identityId: UUID,
      tenantId: UUID,
      category: String,
      date: LocalDate,
      subCategory: Option[String]
  ): Task[MonthlyCountResult]
}

trait Result {
  val identityId: UUID
  val tenantId: UUID
  val category: String
  val date: LocalDate
  val subCategory: Option[String]
}

case class DailyCountResult(
    identityId: UUID,
    tenantId: UUID,
    category: String,
    date: LocalDate,
    subCategory: Option[String],
    year: Int,
    month: Int,
    day: Int,
    count: Long
) extends Result

case class MonthlyCountResult(
    identityId: UUID,
    tenantId: UUID,
    category: String,
    date: LocalDate,
    subCategory: Option[String],
    year: Int,
    month: Int,
    count: Long
) extends Result

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

  override def dailyCount(identityId: UUID, tenantId: UUID, category: String, date: LocalDate, subCategory: Option[String]): Task[MonthlyCountResult] = {

    val tasks = dailyRange(date).par.map { case (day, hour) =>
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

  private def monthRange(date: LocalDate): Seq[(Int, Int)] = (1 to date.lengthOfMonth).flatMap { day =>
    (0 to 23).map { hour =>
      (day, hour)
    }
  }

  private def dailyRange(date: LocalDate): Seq[(Int, Int)] = (0 to 23).map { hour => (date.getDayOfMonth, hour) }

}
