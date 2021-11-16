package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.util.{ Date, UUID }
import javax.inject.Inject

trait AcctEventRowsQueries extends TablePointer[AcctEventRow] {
  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventRow] = schemaMeta[AcctEventRow]("acct_events")

  def insertQ(acctEventRow: AcctEventRow) = quote {
    query[AcctEventRow].insert(lift(acctEventRow))
  }

  def selectAllQ = quote(query[AcctEventRow])

  def byOwnerIdQ(ownerId: UUID) = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .map(x => x)
  }

  def byOwnerIdCountQ(ownerId: UUID) = quote { byOwnerIdQ(ownerId).size }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, identityId: UUID): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.identityId == lift(identityId))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdCountQ(ownerId: UUID, identityId: UUID) = quote { byOwnerIdAndIdentityIdQ(ownerId, identityId).size }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, identityId: UUID, start: Date, end: Date): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.identityId == lift(identityId))
      .filter(_.day >= lift(start))
      .filter(_.day <= lift(end))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdCountQ(ownerId: UUID, identityId: UUID, start: Date, end: Date) = quote {
    byOwnerIdAndIdentityIdQ(ownerId, identityId, start, end).size
  }

  def deleteQ(ownerId: UUID, acctEventId: UUID) = quote {
    query[AcctEventRow].filter(x => x.ownerId == lift(ownerId) && x.id == lift(acctEventId)).delete
  }

}

class AcctEventDAO @Inject() (val connectionService: ConnectionService) extends AcctEventRowsQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def selectAll: Observable[AcctEventRow] = run(selectAllQ)

  def insert(acctEventRow: AcctEventRow): Observable[Unit] = run(insertQ(acctEventRow))

  def byOwnerId(ownerId: UUID): Observable[AcctEventRow] = run(byOwnerIdQ(ownerId))

  def byOwnerIdCount(ownerId: UUID): Observable[Long] = run(byOwnerIdCountQ(ownerId))

  def byOwnerIdAndIdentityId(ownerId: UUID, identityId: UUID): Observable[AcctEventRow] = run(byOwnerIdAndIdentityIdQ(ownerId, identityId))

  def byOwnerIdAndIdentityIdCount(ownerId: UUID, identityId: UUID): Observable[Long] = run(byOwnerIdAndIdentityIdCountQ(ownerId, identityId))

  def byOwnerIdAndIdentityId(ownerId: UUID, identityId: UUID, start: Date, end: Date): Observable[AcctEventRow] = run(byOwnerIdAndIdentityIdQ(ownerId, identityId, start, end))

  def byOwnerIdAndIdentityIdCount(ownerId: UUID, identityId: UUID, start: Date, end: Date): Observable[Long] = run(byOwnerIdAndIdentityIdCountQ(ownerId, identityId, start, end))

  def delete(ownerId: UUID, acctEventId: UUID): Observable[Unit] = run(deleteQ(ownerId, acctEventId))

}
