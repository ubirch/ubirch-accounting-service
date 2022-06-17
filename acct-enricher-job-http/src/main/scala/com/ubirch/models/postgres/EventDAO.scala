package com.ubirch.models.postgres

import com.ubirch.services.formats.JsonConverterService

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ EntityQuery, H2Dialect, PostgresDialect }
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

trait EventDAO {
  def get(tenantId: UUID, category: Option[String], from: LocalDate, to: LocalDate): Task[List[EventRow]]
}

class EventDAOImpl[Dialect <: SqlIdiom](val quillJdbcContext: QuillJdbcContext[Dialect], jsonConverterService: JsonConverterService) extends EventDAO with MapEncoding {

  import quillJdbcContext.ctx._

  implicit val eventRowRowSchemaMeta = schemaMeta[EventRow]("enricher.event")

  implicit class DateQuotes(left: LocalDate) {
    def >=(right: LocalDate) = quote(infix"$left >= $right".as[Boolean])
    def <=(right: LocalDate) = quote(infix"$left <= $right".as[Boolean])
  }

  override def stringifyMap(value: Map[String, String]): String = {
    if (value.isEmpty) ""
    else jsonConverterService.toString(value).toTry.get
  }

  override def toMap(value: String): Map[String, String] = {
    if (value.isEmpty) Map.empty
    else jsonConverterService.as[Map[String, String]](value).toTry.get
  }

  private def get_Q(tenantId: UUID, category: Option[String], from: LocalDate, to: LocalDate): Quoted[EntityQuery[EventRow]] = {
    val q0 = quote {
      query[EventRow]
        .filter(_.tenantId == lift(tenantId))
        .filter(e => e.date >= lift(from) && e.date <= lift(to))
    }

    category match {
      case Some(cat) => quote(q0.filter(_.category == lift(cat)))
      case None => q0
    }
  }

  def get(tenantId: UUID, category: Option[String], from: LocalDate, to: LocalDate): Task[List[EventRow]] = {
    Task.delay(run(get_Q(tenantId, category, from, to)))
  }

}

@Singleton
class DefaultPostgresEventDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect], jsonConverterService: JsonConverterService)
  extends EventDAOImpl(quillJdbcContext, jsonConverterService)

@Singleton
class DefaultEventDAO @Inject() (quillJdbcContext: QuillJdbcContext[H2Dialect], jsonConverterService: JsonConverterService)
  extends EventDAOImpl(quillJdbcContext, jsonConverterService)
