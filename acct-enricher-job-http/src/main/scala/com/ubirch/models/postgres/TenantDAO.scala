package com.ubirch.models.postgres

import com.ubirch.services.formats.JsonConverterService

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ EntityQuery, H2Dialect, PostgresDialect }
import monix.eval.Task

import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

case class TenantRow(
    id: UUID,
    parentId: Option[UUID],
    groupName: String,
    groupPath: String,
    name: Option[String],
    address: Option[String],
    representative: Option[String],
    taxId: Option[String],
    attributes: Map[String, String],
    createdAt: Date,
    updatedAt: Date
)

trait TenantDAO {
  def getSubTenants(tenantId: UUID): Task[List[TenantRow]]
}

class TenantDAOImpl[Dialect <: SqlIdiom](val quillJdbcContext: QuillJdbcContext[Dialect], jsonConverterService: JsonConverterService) extends TenantDAO with MapEncoding {

  import quillJdbcContext.ctx._

  implicit val tenantRowSchemaMeta = schemaMeta[TenantRow]("enricher.tenant")
  implicit val tenantRowInsertMeta = insertMeta[TenantRow]()

  override def stringifyMap(value: Map[String, String]): String = {
    if (value.isEmpty) ""
    else jsonConverterService.toString(value).toTry.get
  }

  override def toMap(value: String): Map[String, String] = {
    if (value.isEmpty) Map.empty
    else jsonConverterService.as[Map[String, String]](value).toTry.get
  }

  private def getSubTenants_Q(tenantId: UUID): Quoted[EntityQuery[TenantRow]] = {
    quote {
      query[TenantRow]
        .filter(_.parentId.isDefined)
        .filter(_.parentId == lift(Option(tenantId)))
    }
  }

  override def getSubTenants(tenantId: UUID): Task[List[TenantRow]] = {
    Task.delay(run(getSubTenants_Q(tenantId: UUID)))
  }
}

@Singleton
class DefaultPostgresTenantDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect], jsonConverterService: JsonConverterService)
  extends TenantDAOImpl(quillJdbcContext, jsonConverterService)

@Singleton
class DefaultTenantDAO @Inject() (quillJdbcContext: QuillJdbcContext[H2Dialect], jsonConverterService: JsonConverterService)
  extends TenantDAOImpl(quillJdbcContext, jsonConverterService)
