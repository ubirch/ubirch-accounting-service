package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService

import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

trait AcctEventRowsByCatQueries extends TablePointer[AcctEventRow] {
  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[AcctEventRow] = schemaMeta[AcctEventRow]("acct_events_by_cat2")

  def selectAllQ: db.Quoted[db.EntityQuery[AcctEventRow]] = quote(query[AcctEventRow])

  def byOwnerIdQ(ownerId: UUID, category: String) = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.category == lift(category))
      .map(x => x)
  }

  def byOwnerIdCountQ(ownerId: UUID, category: String) = quote { byOwnerIdQ(ownerId, category).size }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, category: String, identityId: UUID) = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.category == lift(category))
      .filter(_.identityId == lift(identityId))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdCountQ(ownerId: UUID, category: String, identityId: UUID) = quote {
    byOwnerIdAndIdentityIdQ(ownerId, category, identityId).size
  }

  def byOwnerIdAndIdentityIdQ(ownerId: UUID, category: String, identityId: UUID, start: LocalDate, end: LocalDate) = quote {
    query[AcctEventRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.category == lift(category))
      .filter(_.identityId == lift(identityId))
      .filter(_.day >= lift(start))
      .filter(_.day <= lift(end))
      .map(x => x)
  }

  def byOwnerIdAndIdentityIdCountQ(ownerId: UUID, category: String, identityId: UUID, start: LocalDate, end: LocalDate) = quote {
    byOwnerIdAndIdentityIdQ(ownerId, category, identityId, start, end).size
  }

}

class AcctEventByCatDAO @Inject() (val connectionService: ConnectionService) extends AcctEventRowsByCatQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def selectAll: Observable[AcctEventRow] = run(selectAllQ)

  def byOwnerId(ownerId: UUID, category: String): Observable[AcctEventRow] = run(byOwnerIdQ(ownerId, category))

  def byOwnerIdCount(ownerId: UUID, category: String): Observable[Long] = run(byOwnerIdCountQ(ownerId, category))

  def byOwnerIdAndIdentityId(ownerId: UUID, category: String, identityId: UUID): Observable[AcctEventRow] =
    run(byOwnerIdAndIdentityIdQ(ownerId, category, identityId))

  def byOwnerIdAndIdentityIdCount(ownerId: UUID, category: String, identityId: UUID): Observable[Long] =
    run(byOwnerIdAndIdentityIdCountQ(ownerId, category, identityId))

  def byOwnerIdAndIdentityId(ownerId: UUID, category: String, identityId: UUID, start: LocalDate, end: LocalDate): Observable[AcctEventRow] =
    run(byOwnerIdAndIdentityIdQ(ownerId, category, identityId, start, end))

  def byOwnerIdAndIdentityIdCount(ownerId: UUID, category: String, identityId: UUID, start: LocalDate, end: LocalDate): Observable[Long] =
    run(byOwnerIdAndIdentityIdCountQ(ownerId, category, identityId, start, end))

}
