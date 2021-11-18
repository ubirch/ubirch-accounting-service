package com.ubirch.services

import com.ubirch.models.{ AcctEventByCatDAO, AcctEventDAO, AcctEventRow }

import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.reactive.Observable

import java.time.LocalDate
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait AcctEventsService {
  def byOwnerIdAndIdentityId(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[LocalDate], end: Option[LocalDate]): Observable[AcctEventRow]
  def byOwnerIdAndIdentityIdCount(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[LocalDate], end: Option[LocalDate]): Observable[Long]
  def byOwnerIdAndIdentityIdBucketed(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[LocalDate], end: Option[LocalDate]): Task[Map[String, Int]] = {
    byOwnerIdAndIdentityId(ownerId, category, identityId, start, end)
      .toListL
      .map(_.groupBy(x => x.day.toString))
      .map(_.mapValues(_.size))
  }
}

@Singleton
class DefaultAcctEventsService @Inject() (acctEventDAO: AcctEventDAO, acctEventByCatDAO: AcctEventByCatDAO) extends AcctEventsService with LazyLogging {

  override def byOwnerIdAndIdentityId(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[LocalDate], end: Option[LocalDate]): Observable[AcctEventRow] = {

    byOwnerIdAndIdentityIdBase(ownerId, category, identityId, start, end)(
      a = (ownerId, cat, id, s, e) => acctEventByCatDAO.byOwnerIdAndIdentityId(ownerId, cat, id, s, e),
      b = (ownerId, cat, id) => acctEventByCatDAO.byOwnerIdAndIdentityId(ownerId, cat, id),
      c = (ownerId, cat) => acctEventByCatDAO.byOwnerId(ownerId, cat),
      d = (ownerId, id, s, e) => acctEventDAO.byOwnerIdAndIdentityId(ownerId, id, s, e),
      e = (ownerId, id) => acctEventDAO.byOwnerIdAndIdentityId(ownerId, id),
      f = ownerId => acctEventDAO.byOwnerId(ownerId)
    )

  }

  override def byOwnerIdAndIdentityIdCount(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[LocalDate], end: Option[LocalDate]): Observable[Long] = {
    byOwnerIdAndIdentityIdBase(ownerId, category, identityId, start, end)(
      a = (ownerId, cat, id, s, e) => acctEventByCatDAO.byOwnerIdAndIdentityIdCount(ownerId, cat, id, s, e),
      b = (ownerId, cat, id) => acctEventByCatDAO.byOwnerIdAndIdentityIdCount(ownerId, cat, id),
      c = (ownerId, cat) => acctEventByCatDAO.byOwnerIdCount(ownerId, cat),
      d = (ownerId, id, s, e) => acctEventDAO.byOwnerIdAndIdentityIdCount(ownerId, id, s, e),
      e = (ownerId, id) => acctEventDAO.byOwnerIdAndIdentityIdCount(ownerId, id),
      f = ownerId => acctEventDAO.byOwnerIdCount(ownerId)
    )
  }

  private def byOwnerIdAndIdentityIdBase[T](
      ownerId: UUID,
      category: Option[String],
      identityId: Option[UUID],
      start: Option[LocalDate],
      end: Option[LocalDate]
  )(
      a: (UUID, String, UUID, LocalDate, LocalDate) => Observable[T],
      b: (UUID, String, UUID) => Observable[T],
      c: (UUID, String) => Observable[T],
      d: (UUID, UUID, LocalDate, LocalDate) => Observable[T],
      e: (UUID, UUID) => Observable[T],
      f: (UUID) => Observable[T]
  ): Observable[T] = {

    (category, identityId, start, end) match {
      case (Some(cat), Some(id), Some(s), Some(e)) =>
        a(ownerId, cat, id, s, e)
          .onErrorHandle { e =>
            logger.error("error_cat_a -> ", e)
            throw e
          }
      case (Some(cat), Some(id), None, None) =>
        b(ownerId, cat, id)
          .onErrorHandle { e =>
            logger.error("error_cat_b -> ", e)
            throw e
          }
      case (Some(cat), None, None, None) =>
        c(ownerId, cat)
          .onErrorHandle { e =>
            logger.error("error_cat_c -> ", e)
            throw e
          }
      case (None, Some(id), Some(s), Some(e)) =>
        d(ownerId, id, s, e)
          .onErrorHandle { e =>
            logger.error("error_d -> ", e)
            throw e
          }
      case (None, Some(id), None, None) =>
        e(ownerId, id)
          .onErrorHandle { e =>
            logger.error("error_e -> ", e)
            throw e
          }
      case (None, None, None, None) =>
        f(ownerId)
          .onErrorHandle { e =>
            logger.error("error_f-> ", e)
            throw e
          }

      case _ =>
        Observable.raiseError {
          val e = new IllegalArgumentException(s"owner_id->$ownerId, cat=${category.getOrElse("")}, identity_id->${identityId.getOrElse("")}, start=${start.getOrElse("")}, end=${end.getOrElse("")}")
          logger.error("error -> ", e)
          e
        }

    }

  }
}
