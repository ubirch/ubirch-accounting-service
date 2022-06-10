package com.ubirch.models.postgres

import com.ubirch.services.lifeCycle.Lifecycle

import com.typesafe.scalalogging.LazyLogging
import io.getquill._
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import java.io.Closeable
import javax.inject.{ Inject, Singleton }
import javax.sql.DataSource
import scala.concurrent.Future

trait QuillJdbcContext[Dialect <: SqlIdiom] {
  val ctx: JdbcContext[Dialect, SnakeCase]
  def getDataSource: DataSource with Closeable = ctx.dataSource
}

abstract class QuillJdbcContextImpl[Dialect <: SqlIdiom](lifecycle: Lifecycle) extends QuillJdbcContext[Dialect] with LazyLogging {
  //this is used to try not to start it when it hasn't been started
  protected var started = false
  lifecycle.addStopHook(() => Future.successful {
    if (started) {
      logger.info("Shutting down DB Connection")
      ctx.close()
    }
  })

}

abstract class PostgresContextImpl(lifecycle: Lifecycle) extends QuillJdbcContextImpl[PostgresDialect](lifecycle)

abstract class H2ContextImpl(lifecycle: Lifecycle) extends QuillJdbcContextImpl[H2Dialect](lifecycle)

@Singleton
class DefaultPostgresQuillJdbcContext @Inject() (lifecycle: Lifecycle) extends PostgresContextImpl(lifecycle) with LazyLogging {

  lazy val ctx: PostgresJdbcContext[SnakeCase] =
    try {
      new PostgresJdbcContext(SnakeCase, "system.database")
    } catch {
      case _: IllegalStateException =>
        //This error will contain otherwise password information, which we wouldn't want to log.
        throw new IllegalStateException(
          "something went wrong on constructing postgres jdbc context; we're hiding the original exception message," +
            " so no password will be shown. You might want to activate the error and change the password afterwards."
        )
      case ex: Throwable => throw ex
    } finally {
      started = true
    }

}

@Singleton
class DefaultH2QuillJdbcContext @Inject() (lifecycle: Lifecycle) extends H2ContextImpl(lifecycle) with LazyLogging {

  lazy val ctx: H2JdbcContext[SnakeCase] =
    try {
      new H2JdbcContext(SnakeCase, "system.database")
    } catch {
      case ex: Throwable => throw ex
    } finally {
      started = true
    }

}
