package com.ubirch.models.postgres

import com.ubirch.services.externals.Tenant
import com.ubirch.services.formats.JsonConverterService

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ EntityQuery, H2Dialect, Insert, PostgresDialect }
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

object TenantRow {
  def fromTenant(parent: Option[Tenant], tenant: Tenant) = {
    TenantRow(
      id = UUID.fromString(tenant.id),
      parentId = parent.map(_.id).map(x => UUID.fromString(x)),
      groupName = tenant.name,
      groupPath = tenant.path,
      name = tenant.attributes.get("tenant_name"),
      address = tenant.attributes.get("tenant_address"),
      representative = tenant.attributes.get("tenant_representative"),
      taxId = tenant.attributes.get("tenant_tax_id"),
      attributes = tenant.attributes,
      createdAt = new Date(),
      updatedAt = new Date()
    )
  }
}

trait TenantDAO {
  def store(tenantRow: TenantRow): Task[Unit]
  def getSubTenants: Task[List[TenantRow]]
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

  private def store_Q(tenantRow: TenantRow): Quoted[Insert[TenantRow]] = {
    quote {
      query[TenantRow]
        .insert(lift(tenantRow))
        .onConflictUpdate(_.id)(
          (t, _) => t.groupName -> t.groupName,
          (t, _) => t.groupPath -> t.groupPath,
          (t, _) => t.name -> t.name,
          (t, _) => t.address -> t.address,
          (t, _) => t.representative -> t.representative,
          (t, _) => t.taxId -> t.taxId,
          (t, _) => t.attributes -> t.attributes,
          (t, _) => t.updatedAt -> lift(new Date())
        )
    }
  }

  override def store(tenantRow: TenantRow): Task[Unit] = {
    Task.delay(run(store_Q(tenantRow))).map(_ => ())
  }

  private def getSubTenants_Q: Quoted[EntityQuery[TenantRow]] = {
    quote {
      query[TenantRow]
        .filter(_.parentId.isDefined)
    }
  }

  override def getSubTenants: Task[List[TenantRow]] = {
    Task.delay(run(getSubTenants_Q))
  }
}

@Singleton
class DefaultPostgresTenantDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect], jsonConverterService: JsonConverterService)
  extends TenantDAOImpl(quillJdbcContext, jsonConverterService)

@Singleton
class DefaultTenantDAO @Inject() (quillJdbcContext: QuillJdbcContext[H2Dialect], jsonConverterService: JsonConverterService)
  extends TenantDAOImpl(quillJdbcContext, jsonConverterService)
