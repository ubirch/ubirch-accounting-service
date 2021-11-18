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

    category match {
      case Some(cat) =>

        identityId match {
          case Some(id) =>
            (start, end) match {
              case (Some(s), Some(e)) =>

                acctEventByCatDAO.byOwnerIdAndIdentityId(ownerId, cat, id, s, e)
                  .onErrorHandle { e =>
                    logger.error("error_cat_byOwnerIdAndIdentityId -> ", e)
                    throw e
                  }

              case _ =>

                acctEventByCatDAO.byOwnerIdAndIdentityId(ownerId, cat, id)
                  .onErrorHandle { e =>
                    logger.error("error_cat_byOwnerIdAndIdentityId -> ", e)
                    throw e
                  }
            }

          case None =>
            acctEventByCatDAO.byOwnerId(ownerId, cat)
              .onErrorHandle { e =>
                logger.error("error_cat_byOwnerId -> ", e)
                throw e
              }
        }

      case None =>
        identityId match {
          case Some(id) =>
            (start, end) match {
              case (Some(s), Some(e)) =>

                acctEventDAO.byOwnerIdAndIdentityId(ownerId, id, s, e)
                  .onErrorHandle { e =>
                    logger.error("error_byOwnerIdAndIdentityId -> ", e)
                    throw e
                  }

              case _ =>

                acctEventDAO.byOwnerIdAndIdentityId(ownerId, id)
                  .onErrorHandle { e =>
                    logger.error("error_byOwnerIdAndIdentityId -> ", e)
                    throw e
                  }
            }

          case None => acctEventDAO.byOwnerId(ownerId).onErrorHandle { e =>
            logger.error("error_byOwnerId -> ", e)
            throw e
          }
        }
    }

  }

  override def byOwnerIdAndIdentityIdCount(ownerId: UUID, category: Option[String], identityId: Option[UUID], start: Option[LocalDate], end: Option[LocalDate]): Observable[Long] = {

    category match {
      case Some(cat) =>

        identityId match {
          case Some(id) =>
            (start, end) match {
              case (Some(s), Some(e)) =>

                acctEventByCatDAO.byOwnerIdAndIdentityIdCount(ownerId, cat, id, s, e)
                  .onErrorHandle { e =>
                    logger.error("error_cat_count_byOwnerIdAndIdentityId -> ", e)
                    throw e
                  }

              case _ =>

                acctEventByCatDAO.byOwnerIdAndIdentityIdCount(ownerId, cat, id)
                  .onErrorHandle { e =>
                    logger.error("error_cat_count_byOwnerIdAndIdentityIdCount -> ", e)
                    throw e
                  }
            }

          case None =>
            acctEventByCatDAO.byOwnerIdCount(ownerId, cat)
              .onErrorHandle { e =>
                logger.error("error_cat_count_byOwnerIdCount -> ", e)
                throw e
              }
        }

      case None =>
        identityId match {
          case Some(id) =>
            (start, end) match {
              case (Some(s), Some(e)) =>

                acctEventDAO.byOwnerIdAndIdentityIdCount(ownerId, id, s, e)
                  .onErrorHandle { e =>
                    logger.error("error_cat_count_byOwnerIdAndIdentityIdCount -> ", e)
                    throw e
                  }

              case _ =>

                acctEventDAO.byOwnerIdAndIdentityIdCount(ownerId, id)
                  .onErrorHandle { e =>
                    logger.error("error_cat_count_byOwnerIdAndIdentityIdCount -> ", e)
                    throw e
                  }
            }

          case None =>
            acctEventDAO.byOwnerIdCount(ownerId)
              .onErrorHandle { e =>
                logger.error("error_cat_count_byOwnerIdCount -> ", e)
                throw e
              }
        }
    }
  }
}
