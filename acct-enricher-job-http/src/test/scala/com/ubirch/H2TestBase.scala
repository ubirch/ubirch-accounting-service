package com.ubirch

import com.ubirch.models.postgres.DefaultH2QuillJdbcContext
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import java.nio.file.Paths

trait H2SetupTests extends Awaits with ExecutionContextsTests {
  def cleanUpData(ctx: DefaultH2QuillJdbcContext): Long = {
    ctx.ctx.executeAction("SET REFERENTIAL_INTEGRITY FALSE;")
    val tableNames = Seq("event", "identity", "job", "tenant")
    tableNames.foreach { tableName =>
      ctx.ctx.executeAction("TRUNCATE TABLE enricher." + tableName)
    }
    ctx.ctx.executeAction("SET REFERENTIAL_INTEGRITY TRUE;")
  }

  def migrate(ctx: DefaultH2QuillJdbcContext): MigrateResult = {
    val maybeRoot = Paths.get(".").normalize.toAbsolutePath.toString
    val rootSplit = maybeRoot.split("/")
    val root = if (rootSplit.last == "ubirch-accounting-service") maybeRoot else rootSplit.dropRight(1).mkString("/")

    val flyway = Flyway
      .configure()
      .locations(s"filesystem:$root/acct-enricher-job/src/main/resources/db_sql")
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
