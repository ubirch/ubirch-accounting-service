package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

trait AcctEventCountByDayRowQueries extends TablePointer[AcctEventCountByDayRow] {
  import db._

  //These represent query descriptions only

  implicit val pointingAt = schemaMeta[AcctEventCountByDayRow]("acct_events_counts_by_day")

  def addQ(identityId: UUID, category: String, day: LocalDate) = quote {
    query[AcctEventCountByDayRow]
      .filter(_.identityId == lift(identityId))
      .filter(_.day == lift(day))
      .filter(_.category == lift(category))
      .update(x => x.countEvents -> (x.countEvents + 1L))

  }

  def byIdentityIdAndCategoryQ(identityId: UUID, category: String) = quote {
    query[AcctEventCountByDayRow]
      .filter(_.identityId == lift(identityId))
      .filter(_.category == lift(category))
  }

}

class AcctEventCountByDayDAO @Inject() (val connectionService: ConnectionService) extends AcctEventCountByDayRowQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def byIdentityIdAndCategory(identityId: UUID, category: String): Observable[AcctEventCountByDayRow] =
    run(byIdentityIdAndCategoryQ(identityId, category))

  def add(identityId: UUID, category: String, day: LocalDate): Observable[Unit] =
    run(addQ(identityId, category, day))

}

