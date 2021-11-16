package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.util.{ Date, UUID }
import javax.inject.Inject

trait AcctEventRowsByCatQueries extends TablePointer[AcctEventRow] {
  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventRow] = schemaMeta[AcctEventRow]("acct_events_by_cat")

  def selectAllQ: db.Quoted[db.EntityQuery[AcctEventRow]] = quote(query[AcctEventRow])

  def byOwnerIdQ(ownerId: UUID, category: String): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, category: String, identityId: UUID): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.category == lift(category))
      .filter(_.identityId == lift(identityId))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, category: String, identityId: UUID, start: Date, end: Date): db.Quoted[db.EntityQuery[AcctEventRow]] = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.category == lift(category))
      .filter(_.identityId == lift(identityId))
      .filter(_.day >= lift(start))
      .filter(_.day <= lift(end))
      .map(x => x)
  }

}

class AcctEventByCatDAO @Inject() (val connectionService: ConnectionService) extends AcctEventRowsByCatQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def selectAll: Observable[AcctEventRow] = run(selectAllQ)

  def byOwnerId(ownerId: UUID, category: String): Observable[AcctEventRow] = run(byOwnerIdQ(ownerId, category))

  def byOwnerIdAndIdentityId(ownerId: UUID, category: String, identityId: UUID): Observable[AcctEventRow] = run(byOwnerIdAndIdentityIdQ(ownerId, category, identityId))

  def byOwnerIdAndIdentityId(ownerId: UUID, category: String, identityId: UUID, start: Date, end: Date): Observable[AcctEventRow] = run(byOwnerIdAndIdentityIdQ(ownerId, category, identityId, start, end))

}
