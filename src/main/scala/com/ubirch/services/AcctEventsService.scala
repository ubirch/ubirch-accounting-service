package com.ubirch.services

import com.ubirch.models.{ AcctEventDAO, AcctEventRow }

import monix.reactive.Observable

import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def byOwnerIdAndIdentityId(ownerId: UUID, identityId: Option[UUID]): Observable[AcctEventRow]
}

@Singleton
class DefaultAcctEventsService @Inject() (acctEventDAO: AcctEventDAO) extends AcctEventsService {
  override def byOwnerIdAndIdentityId(ownerId: UUID, identityId: Option[UUID]): Observable[AcctEventRow] = identityId match {
    case Some(id) =>
      acctEventDAO.byOwnerIdAndIdentityId(ownerId, id)
        .onErrorHandle { x =>
          x.printStackTrace()
          throw x
        }

    case None => acctEventDAO.byOwnerId(ownerId)
  }
}
