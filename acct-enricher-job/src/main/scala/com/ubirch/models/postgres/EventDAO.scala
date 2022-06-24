package com.ubirch.models.postgres

import com.ubirch.services.DailyCountResult
import com.ubirch.services.formats.JsonConverterService
import io.getquill.{ H2Dialect, Insert, PostgresDialect, Query }
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
  def getByIdentityId(identityId: UUID): Task[List[EventRow]]
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
          (t, e) => t.count -> e.count,
          (t, _) => t.updatedAt -> lift(new Date())
        )
    }
  }

  override def store(eventRow: EventRow): Task[EventRow] = {
    Task.delay(run(store_Q(eventRow))).map(_ => eventRow)
  }

  override def getByIdentityId(identityId: UUID): Task[List[EventRow]] = {
    Task.delay(run(getByIdentityId_Q(identityId)))
  }

  private def getByIdentityId_Q(identityId: UUID): Quoted[Query[EventRow]] = {
    quote {
      query[EventRow]
        .filter(_.identityId == lift(identityId))
    }
  }
}

@Singleton
class DefaultPostgresEventDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect], jsonConverterService: JsonConverterService)
  extends EventDAOImpl(quillJdbcContext, jsonConverterService)

@Singleton
class DefaultEventDAO @Inject() (quillJdbcContextH2: QuillJdbcContext[H2Dialect], jsonConverterService: JsonConverterService)
  extends EventDAOImpl(quillJdbcContextH2, jsonConverterService) {
  import this.quillJdbcContext.ctx._

  private def store_Q(eventRow: EventRow): Quoted[Insert[EventRow]] = {
    quote {
      query[EventRow]
        .insert(lift(eventRow))
    }
  }

  override def store(eventRow: EventRow): Task[EventRow] = {
    Task.delay(run(store_Q(eventRow))).map(_ => eventRow)
  }
}
