package com.ubirch.services

import com.ubirch.models.{ AcctEventByCatDAO, AcctEventDAO, AcctEventRow }

import monix.reactive.Observable

import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def byOwnerIdAndIdentityId(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[Date], end: Option[Date]): Observable[AcctEventRow]
}

@Singleton
class DefaultAcctEventsService @Inject() (acctEventDAO: AcctEventDAO, acctEventByCatDAO: AcctEventByCatDAO) extends AcctEventsService {
  override def byOwnerIdAndIdentityId(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[Date], end: Option[Date]): Observable[AcctEventRow] = {

    category match {
      case Some(cat) =>

        identityId match {
          case Some(id) =>
            (start, end) match {
              case (Some(s), Some(e)) =>

                acctEventByCatDAO.byOwnerIdAndIdentityId(ownerId, cat, id, s, e)
                  .onErrorHandle { x =>
                    x.printStackTrace()
                    throw x
                  }

              case _ =>

                acctEventByCatDAO.byOwnerIdAndIdentityId(ownerId, cat, id)
                  .onErrorHandle { x =>
                    x.printStackTrace()
                    throw x
                  }
            }

          case None => acctEventByCatDAO.byOwnerId(ownerId, cat)
        }

      case None =>
        identityId match {
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

  }

}
