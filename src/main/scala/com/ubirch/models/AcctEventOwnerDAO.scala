package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import javax.inject.Inject

trait AcctEventOwnerRowsQueries extends TablePointer[AcctEventOwnerRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventOwnerRow] =
    schemaMeta[AcctEventOwnerRow]("acct_event_owners")

  def insertQ(acctEventOwnerRow: AcctEventOwnerRow) = quote {
    query[AcctEventOwnerRow].insert(lift(acctEventOwnerRow))
  }

}

class AcctEventOwnerDAO @Inject() (val connectionService: ConnectionService) extends AcctEventOwnerRowsQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def insert(acctEventOwnerRow: AcctEventOwnerRow): Observable[Unit] = run(insertQ(acctEventOwnerRow))

}
