package com.ubirch.models.postgres

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ H2Dialect, Insert, PostgresDialect, Query }
import monix.eval.Task
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException

import java.time.LocalDate
import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

case class JobRow(
    id: UUID,
    success: Option[Boolean],
    queryDays: String,
    startedAt: Date,
    endedAt: Option[Date],
    comment: Option[String],
    createdAt: Date,
    updatedAt: Date
) {
  def end(success: Boolean, comment: Option[String]): JobRow = copy(success = Option(success), endedAt = Option(new Date()), comment = comment.filter(_.nonEmpty), updatedAt = new Date())
}

object JobRow {
  def apply(queryDays: List[LocalDate]): JobRow =
    new JobRow(id = UUID.randomUUID(), success = None, queryDays = queryDays.mkString(","), startedAt = new Date(), endedAt = None, comment = None, createdAt = new Date(), updatedAt = new Date())
}

trait JobDAO {
  def store(jobRow: JobRow): Task[JobRow]
  def getLatestJob: Task[Option[JobRow]]
}

class JobDAOImpl[Dialect <: SqlIdiom](val quillJdbcContext: QuillJdbcContext[Dialect]) extends JobDAO {
  import quillJdbcContext.ctx._

  implicit val jobRowSchemaMeta = schemaMeta[JobRow]("enricher.job")
  implicit val jobRowInsertMeta = insertMeta[JobRow]()

  private def store_Q(jobRow: JobRow): Quoted[Insert[JobRow]] = {
    quote {
      query[JobRow]
        .insert(lift(jobRow))
        .onConflictUpdate(_.id)(
          (t, e) => t.success -> e.success,
          (t, e) => t.comment -> e.comment,
          (t, e) => t.endedAt -> e.endedAt,
          (t, _) => t.updatedAt -> lift(new Date())
        )
    }
  }

  override def store(jobRow: JobRow): Task[JobRow] = {
    Task.delay(run(store_Q(jobRow))).map(_ => jobRow)
  }

  override def getLatestJob: Task[Option[JobRow]] = {
    Task(run(getLatestJob_Q)).map(_.headOption)
  }

  private def getLatestJob_Q: Quoted[Query[JobRow]] = {
    quote {
      query[JobRow]
        .sortBy(_.createdAt)
        .take(1)
    }
  }

}

@Singleton
class DefaultPostgresJobDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect])
  extends JobDAOImpl(quillJdbcContext)

@Singleton
class DefaultH2JobDAO @Inject() (quillJdbcContextH2: QuillJdbcContext[H2Dialect])
  extends JobDAOImpl(quillJdbcContextH2) {
  import this.quillJdbcContext.ctx._
  private def store_Q(jobRow: JobRow): Quoted[Insert[JobRow]] = {
    quote {
      query[JobRow]
        .insert(lift(jobRow))
    }
  }

  private def update_Q(jobRow: JobRow) = {
    quote {
      query[JobRow]
        .filter(_.id == lift(jobRow.id))
        .update(
          _.endedAt -> lift(jobRow.endedAt),
          _.success -> lift(jobRow.success),
          _.comment -> lift(jobRow.comment),
          _.updatedAt -> lift(jobRow.updatedAt),
          _.createdAt -> lift(jobRow.createdAt),
          _.queryDays -> lift(jobRow.queryDays),
          _.startedAt -> lift(jobRow.startedAt)
        )
    }
  }

  override def store(jobRow: JobRow): Task[JobRow] = {
    Task.delay(run(store_Q(jobRow))).map(_ => jobRow).onErrorRecoverWith {
      case _: JdbcSQLIntegrityConstraintViolationException =>
        Task.delay(run(update_Q(jobRow))).map(_ => jobRow)
    }
  }
}

