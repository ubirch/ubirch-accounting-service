package com.ubirch.models.postgres

import io.getquill.{ H2Dialect, PostgresDialect }
import io.getquill.context.sql.idiom.SqlIdiom

import java.util.{ Date, UUID }
import javax.inject.{ Inject, Singleton }

case class IdentityRow(
    id: UUID,
    keycloak: UUID,
    tenantId: UUID,
    description: Option[String],
    attributes: Map[String, String],
    createdAt: Date,
    updatedAt: Date
)

trait IdentityDAO {

}

class IdentityDAOImpl[Dialect <: SqlIdiom](val quillJdbcContext: QuillJdbcContext[Dialect]) extends IdentityDAO {

  import quillJdbcContext.ctx._

  implicit val identityRowSchemaMeta = schemaMeta[IdentityRow]("enricher.identity")

}

@Singleton
class DefaultPostgresIdentityDAO @Inject() (quillJdbcContext: QuillJdbcContext[PostgresDialect])
  extends IdentityDAOImpl(quillJdbcContext)

@Singleton
class DefaultIdentityDAO @Inject() (quillJdbcContext: QuillJdbcContext[H2Dialect])
  extends IdentityDAOImpl(quillJdbcContext)
