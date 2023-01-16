package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventRowsQueries extends CassandraBase[AcctEventRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventRow] = schemaMeta[AcctEventRow]("acct_events")

  def insertQ(acctEventRow: AcctEventRow) = quote {
    query[AcctEventRow].insertValue(lift(acctEventRow))
  }

  def selectAllQ = quote(query[AcctEventRow])

  def byTimeQ(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: Option[String]
  ) = {
    {
      val q0 = quote {
        query[AcctEventRow]
          .filter(_.identityId == lift(identityId))
          .filter(_.category == lift(category))
          .filter(_.year == lift(year))
          .filter(_.month == lift(month))
          .filter(_.day == lift(day))
          .filter(_.hour == lift(hour))
      }
      subCategory match {
        case Some(subCategory) => quote { q0.filter(_.subCategory == lift(subCategory)).map(x => x) }
        case None => quote { q0.map(x => x) }
      }
    }
  }

  def countQ(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: Option[String]
  ) = quote {
    byTimeQ(identityId, category, year, month, day, hour, subCategory).size
  }

}

@Singleton
class AcctEventDAO @Inject() (val connectionService: ConnectionService) extends AcctEventRowsQueries {
  val db: CassandraStreamContext[SnakeCase] = connectionService.context

  import db._

  def selectAll: Observable[AcctEventRow] = run(selectAllQ)

  def insert(acctEventRow: AcctEventRow): Observable[Unit] = run(insertQ(acctEventRow))

  def count(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: Option[String]
  ): Observable[Long] = run(countQ(identityId, category, year, month, day, hour, subCategory))

  def byTime(
      identityId: UUID,
      category: String,
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      subCategory: Option[String]
  ): Observable[AcctEventRow] = run(byTimeQ(identityId, category, year, month, day, hour, subCategory))

}
