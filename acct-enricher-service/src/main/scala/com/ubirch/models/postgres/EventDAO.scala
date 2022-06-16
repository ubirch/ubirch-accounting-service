package com.ubirch.models.postgres

import com.ubirch.services.DailyCountResult
import com.ubirch.services.formats.JsonConverterService

import io.getquill.{ H2Dialect, Insert, PostgresDialect }
import io.getquill.context.sql.idiom.SqlIdiom
import monix.eval.Task

import java.time.LocalDate
import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

case class EventRow(
    identityId: UUID,
    tenantId: UUID,
    category: String,
    date: LocalDate,
    count: Int,
    createdAt: Date,
    updatedAt: Date
)

object EventRow {
  def fromDailyCountResult(result: DailyCountResult) =
    EventRow(
      identityId = result.identityId,
      tenantId = result.tenantId,
      category = result.category,
      date = result.date,
      count = result.count.toInt,
      createdAt = new Date(),
      updatedAt = new Date()
    )
}

trait EventDAO {
  def store(eventRow: EventRow): Task[EventRow]
}

class EventDAOImpl[Dialect <: SqlIdiom](val quillJdbcContext: QuillJdbcContext[Dialect], jsonConverterService: JsonConverterService) extends EventDAO with MapEncoding {

  import quillJdbcContext.ctx._

  implicit val eventRowRowSchemaMeta = schemaMeta[EventRow]("enricher.event")
  implicit val eventRowRowInsertMeta = insertMeta[EventRow]()

  override def stringifyMap(value: Map[String, String]): String = {
    if (value.isEmpty) ""
    else jsonConverterService.toString(value).toTry.get
  }

  override def toMap(value: String): Map[String, String] = {
    if (value.isEmpty) Map.empty
    else jsonConverterService.as[Map[String, String]](value).toTry.get
  }

  private def store_Q(eventRow: EventRow): Quoted[Insert[EventRow]] = {
    quote {
      query[EventRow]
        .insert(lift(eventRow))
        .onConflictUpdate(_.identityId, _.tenantId, _.category, _.date)(
          (t, _) => t.count -> t.count,
          (t, _) => t.updatedAt -> lift(new Date())
        )
    }
  }

  override def store(eventRow: EventRow): Task[EventRow] = {
    Task.delay(run(store_Q(eventRow))).map(_ => eventRow)
  }
}

@Singleton
class DefaultPostgresEventDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect], jsonConverterService: JsonConverterService)
  extends EventDAOImpl(quillJdbcContext, jsonConverterService)

@Singleton
class DefaultEventDAO @Inject() (quillJdbcContext: QuillJdbcContext[H2Dialect], jsonConverterService: JsonConverterService)
  extends EventDAOImpl(quillJdbcContext, jsonConverterService)
