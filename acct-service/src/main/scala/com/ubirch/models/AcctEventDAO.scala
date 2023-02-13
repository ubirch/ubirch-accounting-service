package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.util.UUID
import javax.inject.{ Inject, Singleton }

/**
  * @important
  * Since at least quill 3.12, dynamic query might leads to OutOfMemory.
  * Therefore, we need to avoid using it.
  * @see [[https://github.com/zio/zio-quill/issues/2484]]
  */
trait AcctEventRowsQueries extends CassandraBase[AcctEventRow] {

  import db._

  def insertQ(acctEventRow: AcctEventRow) = quote {
    querySchema[AcctEventRow]("acct_events").insertValue(lift(acctEventRow))
  }

  def selectAllQ = quote(querySchema[AcctEventRow]("acct_events"))

  def byTimesQ(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int
  ) = {
    quote {
      querySchema[AcctEventRow]("acct_events")
        .filter(_.identityId == lift(identityId))
        .filter(_.category == lift(category))
        .filter(_.year == lift(year))
        .filter(_.month == lift(month))
        .filter(_.day == lift(day))
        .filter(_.hour == lift(hour))
        .map(x => x)
    }
  }

  def byTimesQWithSubCategory(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: String
  ) = {
    quote {
      querySchema[AcctEventRow]("acct_events")
        .filter(_.identityId == lift(identityId))
        .filter(_.category == lift(category))
        .filter(_.year == lift(year))
        .filter(_.month == lift(month))
        .filter(_.day == lift(day))
        .filter(_.hour == lift(hour))
        .filter(_.subCategory == lift(subCategory)).map(x => x)
    }
  }
}

@Singleton
class AcctEventDAO @Inject() (val connectionService: ConnectionService) extends AcctEventRowsQueries {
  val db: CassandraStreamContext[SnakeCase] = connectionService.context

  import db._

  def selectAll: Observable[AcctEventRow] = run(selectAllQ)

  def insert(acctEventRow: AcctEventRow): Observable[Unit] = run(insertQ(acctEventRow))

  /**
    * @important
    * we need to do case match here to avoid using dynamic queries
    */
  def count(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: Option[String]
  ): Observable[Long] =
    subCategory match {
      case Some(subCategory) => run(byTimesQWithSubCategory(identityId, category, year, month, day, hour, subCategory).size)
      case None => run(byTimesQ(identityId, category, year, month, day, hour).size)
    }

  /**
    * @important
    * we need to do case match here to avoid using dynamic queries
    */
  def byTime(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: Option[String]
  ): Observable[AcctEventRow] =
    subCategory match {
      case Some(subCategory) => run(byTimesQWithSubCategory(identityId, category, year, month, day, hour, subCategory))
      case None => run(byTimesQ(identityId, category, year, month, day, hour))
    }

}
