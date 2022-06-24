package com.ubirch.models.postgres

import com.ubirch.services.externals.Identity
import com.ubirch.services.formats.JsonConverterService
import io.getquill.{ H2Dialect, Insert, PostgresDialect, Query }
import io.getquill.context.sql.idiom.SqlIdiom
import monix.eval.Task

import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

case class IdentityRow(
    id: UUID,
    keycloakId: UUID,
    tenantId: UUID,
    description: Option[String],
    attributes: Map[String, String],
    createdAt: Date,
    updatedAt: Date
)

object IdentityRow {
  def fromIdentity(identity: Identity) = {
    require(identity.tenantId.nonEmpty, "Identity tenant id can't be empty")
    IdentityRow(
      id = UUID.fromString(identity.deviceId),
      keycloakId = UUID.fromString(identity.keycloakId),
      tenantId = identity.tenantId.get,
      description = Option(identity.description),
      attributes = identity.attributes,
      createdAt = new Date(),
      updatedAt = new Date()
    )
  }
}

trait IdentityDAO {
  def store(identityRow: IdentityRow): Task[IdentityRow]
  def getByTenantId(tenantId: UUID): Task[List[IdentityRow]]
}

class IdentityDAOImpl[Dialect <: SqlIdiom](val quillJdbcContext: QuillJdbcContext[Dialect], jsonConverterService: JsonConverterService) extends IdentityDAO with MapEncoding {

  import quillJdbcContext.ctx._

  implicit val identityRowSchemaMeta = schemaMeta[IdentityRow]("enricher.identity")
  implicit val identityRowInsertMeta = insertMeta[IdentityRow]()

  override def stringifyMap(value: Map[String, String]): String = {
    if (value.isEmpty) ""
    else jsonConverterService.toString(value).toTry.get
  }

  override def toMap(value: String): Map[String, String] = {
    if (value.isEmpty) Map.empty
    else jsonConverterService.as[Map[String, String]](value).toTry.get
  }

  private def store_Q(identityRow: IdentityRow): Quoted[Insert[IdentityRow]] = {
    quote {
      query[IdentityRow]
        .insert(lift(identityRow))
        .onConflictUpdate(_.id)(
          (t, e) => t.description -> e.description,
          (t, e) => t.attributes -> e.attributes,
          (t, _) => t.updatedAt -> lift(new Date())
        )
    }
  }

  override def store(identityRow: IdentityRow): Task[IdentityRow] = {
    Task.delay(run(store_Q(identityRow))).map(_ => identityRow)
  }

  private def getByTenantId_Q(tenantId: UUID): Quoted[Query[IdentityRow]] = {
    quote {
      query[IdentityRow]
        .filter(_.tenantId == lift(tenantId))
    }
  }

  override def getByTenantId(tenantId: UUID): Task[List[IdentityRow]] = {
    Task.delay(run(getByTenantId_Q(tenantId)))
  }
}

@Singleton
class DefaultPostgresIdentityDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect], jsonConverterService: JsonConverterService)
  extends IdentityDAOImpl(quillJdbcContext, jsonConverterService)

@Singleton
class DefaultIdentityDAO @Inject() (quillJdbcContextH2: QuillJdbcContext[H2Dialect], jsonConverterService: JsonConverterService)
  extends IdentityDAOImpl(quillJdbcContextH2, jsonConverterService) {
  import this.quillJdbcContext.ctx._

  private def store_Q(identityRow: IdentityRow): Quoted[Insert[IdentityRow]] = {
    quote {
      query[IdentityRow]
        .insert(lift(identityRow))
    }
  }

  override def store(identityRow: IdentityRow): Task[IdentityRow] = {
    Task.delay(run(store_Q(identityRow))).map(_ => identityRow)
  }
}
