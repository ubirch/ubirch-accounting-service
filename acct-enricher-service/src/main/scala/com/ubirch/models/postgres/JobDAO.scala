package com.ubirch.models.postgres

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ H2Dialect, Insert, PostgresDialect }
import monix.eval.Task

import java.time.LocalDate
import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

case class JobRow(
    id: UUID,
    success: Option[Boolean],
    queryDays: String,
    startedAt: Date,
    endedAt: Option[Date],
    createdAt: Date,
    updatedAt: Date
) {
  def end(success: Boolean): JobRow = copy(success = Option(success), endedAt = Option(new Date()), updatedAt = new Date())
}

object JobRow {
  def apply(queryDays: List[LocalDate]): JobRow =
    new JobRow(UUID.randomUUID(), None, queryDays.mkString(","), new Date(), None, new Date(), new Date())
}

trait JobDAO {
  def store(jobRow: JobRow): Task[JobRow]
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
          (t, e) => t.endedAt -> e.endedAt,
          (t, _) => t.updatedAt -> lift(new Date())
        )
    }
  }

  override def store(jobRow: JobRow): Task[JobRow] = {
    Task.delay(run(store_Q(jobRow))).map(_ => jobRow)
  }

}

@Singleton
class DefaultPostgresJobDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect])
  extends JobDAOImpl(quillJdbcContext)

@Singleton
class DefaultJobDAO @Inject() (quillJdbcContext: QuillJdbcContext[H2Dialect])
  extends JobDAOImpl(quillJdbcContext)

