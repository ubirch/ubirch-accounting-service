package com.ubirch

import com.ubirch.models.postgres.DefaultH2QuillJdbcContext
import io.getquill.context.ExecutionInfo
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.output.MigrateResult

trait H2SetupTests extends Awaits with ExecutionContextsTests {
  def cleanUpData(ctx: DefaultH2QuillJdbcContext): Long = {
    ctx.ctx.executeAction("SET REFERENTIAL_INTEGRITY FALSE;")(ExecutionInfo.unknown, ())
    val tableNames = Seq("event", "identity", "job", "tenant")
    tableNames.foreach { tableName =>
      ctx.ctx.executeAction("TRUNCATE TABLE enricher." + tableName)(ExecutionInfo.unknown, ())
    }
    ctx.ctx.executeAction("SET REFERENTIAL_INTEGRITY TRUE;")(ExecutionInfo.unknown, ())
  }

  def migrate(ctx: DefaultH2QuillJdbcContext): MigrateResult = {
    val flyway = Flyway
      .configure()
      .locations(new Location("classpath:db_sql"))
      .dataSource(ctx.getDataSource)
      .schemas("enricher")
      .load()

    flyway.clean()
    flyway.migrate()
  }
}

trait TestBaseWithH2 extends TestBase with H2SetupTests {
  val injector: InjectorHelper = getInjector

  val ctx: DefaultH2QuillJdbcContext = injector.get[DefaultH2QuillJdbcContext]

  override protected def beforeEach(): Unit = {
    cleanUpData(ctx)
    super.beforeEach()
  }

  override protected def beforeAll(): Unit = {
    migrate(ctx)
    super.beforeAll()
  }
}
