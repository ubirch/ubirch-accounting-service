package com.ubirch.models

import java.util.UUID

import com.ubirch.services.cluster.ConnectionService
import io.getquill.{ CassandraStreamContext, SnakeCase }
import javax.inject.Inject
import monix.reactive.Observable

trait AcctEventRowsQueries extends TablePointer[AcctEventRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventRow] = schemaMeta[AcctEventRow]("acct_events")

  def insertQ(acctEventRow: AcctEventRow): db.Quoted[db.Insert[AcctEventRow]] = quote {
    query[AcctEventRow].insert(lift(acctEventRow))
  }

  def selectAllQ: db.Quoted[db.EntityQuery[AcctEventRow]] = quote(query[AcctEventRow])

  def byOwnerIdQ(ownerId: UUID): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, identityId: UUID): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.identityId == lift(identityId))
      .map(x => x)
  }

  def deleteQ(ownerId: UUID, acctEventId: UUID): db.Quoted[db.Delete[AcctEventRow]] = quote {
    query[AcctEventRow].filter(x => x.ownerId == lift(ownerId) && x.id == lift(acctEventId)).delete
  }

}

class AcctEventDAO @Inject() (val connectionService: ConnectionService) extends AcctEventRowsQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def selectAll: Observable[AcctEventRow] = run(selectAllQ)

  def insert(acctEventRow: AcctEventRow): Observable[Unit] = run(insertQ(acctEventRow))

  def byOwnerId(ownerId: UUID): Observable[AcctEventRow] = run(byOwnerIdQ(ownerId))

  def byOwnerIdAndIdentityId(ownerId: UUID, identityId: UUID): Observable[AcctEventRow] = run(byOwnerIdAndIdentityIdQ(ownerId, identityId))

  def delete(ownerId: UUID, acctEventId: UUID): Observable[Unit] = run(deleteQ(ownerId, acctEventId))

}
