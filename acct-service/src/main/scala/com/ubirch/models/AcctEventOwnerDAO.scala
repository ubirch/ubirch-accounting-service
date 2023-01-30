package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventOwnerRowsQueries extends CassandraBase[AcctEventOwnerRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventOwnerRow] =
    schemaMeta[AcctEventOwnerRow]("acct_event_owners")

  def insertQ(acctEventOwnerRow: AcctEventOwnerRow) = quote {
    query[AcctEventOwnerRow].insertValue(lift(acctEventOwnerRow))
  }

  def byOwnerIdQ(ownerId: UUID) = quote {
    query[AcctEventOwnerRow].filter(_.ownerId == lift(ownerId))
  }

}

@Singleton
class AcctEventOwnerDAO @Inject() (val connectionService: ConnectionService) extends AcctEventOwnerRowsQueries {
  val db: CassandraStreamContext[SnakeCase] = connectionService.context

  import db._

  def insert(acctEventOwnerRow: AcctEventOwnerRow): Observable[Unit] = run(insertQ(acctEventOwnerRow))

  def byOwnerId(ownerId: UUID): Observable[AcctEventOwnerRow] = {
    run(byOwnerIdQ(ownerId))
  }

}
