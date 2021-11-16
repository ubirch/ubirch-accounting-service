package com.ubirch.services

import com.ubirch.models.{ AcctEventDAO, AcctEventRow }

import monix.reactive.Observable

import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def byOwnerIdAndIdentityId(ownerId: UUID, identityId: Option[UUID], start: Option[Date], end: Option[Date]): Observable[AcctEventRow]
}

@Singleton
class DefaultAcctEventsService @Inject() (acctEventDAO: AcctEventDAO) extends AcctEventsService {
  override def byOwnerIdAndIdentityId(ownerId: UUID, identityId: Option[UUID], start: Option[Date], end: Option[Date]): Observable[AcctEventRow] = identityId match {
    case Some(id) =>
      (start, end) match {
        case (Some(s), Some(e)) =>

          acctEventDAO.byOwnerIdAndIdentityId(ownerId, id, s, e)
            .onErrorHandle { x =>
              x.printStackTrace()
              throw x
            }

        case _ =>

          acctEventDAO.byOwnerIdAndIdentityId(ownerId, id)
            .onErrorHandle { x =>
              x.printStackTrace()
              throw x
            }
      }

    case None => acctEventDAO.byOwnerId(ownerId)
  }

}
