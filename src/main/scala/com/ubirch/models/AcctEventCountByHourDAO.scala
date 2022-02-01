package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.time.{ LocalDate, LocalTime }
import java.util.UUID
import javax.inject.Inject

trait AcctEventCountByHourRowQueries extends CassandraBase[AcctEventCountByHourRow] {
  import db._

  //These represent query descriptions only

  implicit val pointingAt = schemaMeta[AcctEventCountByHourRow]("acct_events_counts_by_hour")

  def addQ(identityId: UUID, category: String, day: LocalDate, time: LocalTime) = quote {
    query[AcctEventCountByHourRow]
      .filter(_.identityId == lift(identityId))
      .filter(_.day == lift(day))
      .filter(_.time == lift(time))
      .filter(_.category == lift(category))
      .update(x => x.countEvents -> (x.countEvents + 1L))

  }

  def byIdentityIdAndCategoryQ(identityId: UUID, category: String) = quote {
    query[AcctEventCountByHourRow]
      .filter(_.identityId == lift(identityId))
      .filter(_.category == lift(category))
  }

}

class AcctEventCountByHourDAO @Inject() (val connectionService: ConnectionService) extends AcctEventCountByHourRowQueries {
  val db: CassandraStreamContext[SnakeCase] = connectionService.context

  import db._

  def byIdentityIdAndCategory(identityId: UUID, category: String): Observable[AcctEventCountByHourRow] =
    run(byIdentityIdAndCategoryQ(identityId, category))

  def add(identityId: UUID, category: String, day: LocalDate, hour: LocalTime): Observable[Unit] = {
    run(addQ(identityId, category, day, hour))
  }

}

