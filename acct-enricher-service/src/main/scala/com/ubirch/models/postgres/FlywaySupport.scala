package com.ubirch.models.postgres

import com.ubirch.ConfPaths.PostgresPaths

import com.google.inject.Inject
import com.typesafe.config.Config
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ H2Dialect, PostgresDialect }
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{ CleanResult, MigrateResult }
import org.flywaydb.core.api.{ Location, MigrationInfoService }

import javax.inject.Singleton

trait FlywaySupport {
  def getFlyway(locations: List[Location]): Flyway
  def getFlyway: Flyway
  def migrateWhenOn(): Option[MigrateResult] = if (isOn) Option(getFlyway.migrate()) else None
  def clean(): CleanResult = getFlyway.clean()
  def migrate(): MigrateResult = getFlyway.migrate()
  def info(): MigrationInfoService = getFlyway.info()
  def pending(): Boolean = info().pending().nonEmpty
  def isOn: Boolean
}

abstract class FlywaySupportImpl[Dialect <: SqlIdiom](conf: Config, quillJdbcContext: QuillJdbcContext[Dialect]) extends FlywaySupport with PostgresPaths {

  private val migrateOnStart = conf.getBoolean(MIGRATE_ON_START)

  def suffixes: List[String] = List(".sql")

  def locations: List[Location] = List(new Location("classpath:db"))

  private var flyway: Option[Flyway] = None

  override def getFlyway(locations: List[Location]): Flyway = {
    flyway match {
      case Some(existingFlyway) => existingFlyway
      case None =>
        val newFlyway = Flyway
          .configure()
          .locations(locations: _*)
          .sqlMigrationSuffixes(suffixes: _*)
          .dataSource(quillJdbcContext.getDataSource)
          .schemas("enricher")
          .load()

        flyway = Some(newFlyway)

        newFlyway
    }

  }

  override def isOn: Boolean = migrateOnStart

  override def getFlyway: Flyway = if (isOn) flyway.getOrElse(getFlyway(locations)) else throw new ExceptionInInitializerError("Migrate on start is disabled.")

}

@Singleton
class DefaultPostgresFlywaySupport @Inject() (conf: Config, quillJdbcContext: QuillJdbcContext[PostgresDialect]) extends FlywaySupportImpl[PostgresDialect](conf, quillJdbcContext) {
  override def suffixes: List[String] = super.suffixes ++ List(".sql.ps")
}

@Singleton
class DefaultH2FlywaySupport @Inject() (conf: Config, quillJdbcContext: QuillJdbcContext[H2Dialect]) extends FlywaySupportImpl[H2Dialect](conf, quillJdbcContext) {
  override def suffixes: List[String] = super.suffixes ++ List(".sql.h2")
}
