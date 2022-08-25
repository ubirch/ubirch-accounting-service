package com.ubirch

import com.ubirch.models.postgres.{ DefaultPostgresEventDAO, DefaultPostgresFlywaySupport, DefaultPostgresIdentityDAO, DefaultPostgresJobDAO, DefaultPostgresQuillJdbcContext, DefaultPostgresTenantDAO, EventDAO, FlywaySupport, IdentityDAO, JobDAO, QuillJdbcContext, TenantDAO }
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.jwt._
import com.ubirch.services.lifeCycle.{ DefaultJVMHook, DefaultLifecycle, JVMHook, Lifecycle }
import com.ubirch.services.rest.SwaggerProvider
import com.ubirch.services.{ DefaultSummaryService, SummaryService }
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.{ AbstractModule, Module, TypeLiteral }
import com.typesafe.config.Config
import io.getquill.PostgresDialect
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
  * Represents the default binder for the system components
  */
class Binder
  extends AbstractModule {

  def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(classOf[ConfigProvider])
  def ExecutionContext: ScopedBindingBuilder = bind(classOf[ExecutionContext]).toProvider(classOf[ExecutionProvider])
  def Scheduler: ScopedBindingBuilder = bind(classOf[Scheduler]).toProvider(classOf[SchedulerProvider])
  def Swagger: ScopedBindingBuilder = bind(classOf[Swagger]).toProvider(classOf[SwaggerProvider])
  def Formats: ScopedBindingBuilder = bind(classOf[Formats]).toProvider(classOf[JsonFormatsProvider])
  def Lifecycle: ScopedBindingBuilder = bind(classOf[Lifecycle]).to(classOf[DefaultLifecycle])
  def JVMHook: ScopedBindingBuilder = bind(classOf[JVMHook]).to(classOf[DefaultJVMHook])
  def JsonConverterService: ScopedBindingBuilder = bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
  def TokenCreationService: ScopedBindingBuilder = bind(classOf[TokenCreationService]).to(classOf[DefaultTokenCreationService])
  def TokenVerificationService: ScopedBindingBuilder = bind(classOf[TokenVerificationService]).to(classOf[DefaultTokenVerificationService])
  def PublicKeyDiscoveryService: ScopedBindingBuilder = bind(classOf[PublicKeyDiscoveryService]).to(classOf[DefaultPublicKeyDiscoveryService])
  def PublicKeyPoolService: ScopedBindingBuilder = bind(classOf[PublicKeyPoolService]).to(classOf[DefaultPublicKeyPoolService])
  def SummaryService: ScopedBindingBuilder = bind(classOf[SummaryService]).to(classOf[DefaultSummaryService])
  def QuillJdbcContext: ScopedBindingBuilder = bind(new TypeLiteral[QuillJdbcContext[PostgresDialect]]() {}).to(classOf[DefaultPostgresQuillJdbcContext])
  def TenantDAO: ScopedBindingBuilder = bind(classOf[TenantDAO]).to(classOf[DefaultPostgresTenantDAO])
  def IdentityDAO: ScopedBindingBuilder = bind(classOf[IdentityDAO]).to(classOf[DefaultPostgresIdentityDAO])
  def EventDAO: ScopedBindingBuilder = bind(classOf[EventDAO]).to(classOf[DefaultPostgresEventDAO])
  def JobDAO: ScopedBindingBuilder = bind(classOf[JobDAO]).to(classOf[DefaultPostgresJobDAO])
  def FlywaySupport: ScopedBindingBuilder = bind(classOf[FlywaySupport]).to(classOf[DefaultPostgresFlywaySupport])

  override def configure(): Unit = {
    Config
    ExecutionContext
    Scheduler
    Swagger
    Formats
    Lifecycle
    JVMHook
    JsonConverterService
    TokenVerificationService
    PublicKeyDiscoveryService
    PublicKeyPoolService
    SummaryService
    QuillJdbcContext
    TenantDAO
    IdentityDAO
    EventDAO
    JobDAO
    FlywaySupport
    ()
  }

}

object Binder {
  def modules: List[Module] = List(new Binder)
}
