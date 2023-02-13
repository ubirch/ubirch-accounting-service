package com.ubirch.models.cassandra

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventOwnerRowsQueries extends CassandraBase[AcctEventOwnerRow] {

  import db._

  implicit val pointingAt: db.SchemaMeta[AcctEventOwnerRow] =
    schemaMeta[AcctEventOwnerRow]("acct_event_owners")

  def insertQ(acctEventOwnerRow: AcctEventOwnerRow) = quote {
    querySchema[AcctEventOwnerRow]("acct_event_owners").insertValue(lift(acctEventOwnerRow))
  }

  def byOwnerIdQ(ownerId: UUID) = quote {
    querySchema[AcctEventOwnerRow]("acct_event_owners").filter(_.ownerId == lift(ownerId))
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
