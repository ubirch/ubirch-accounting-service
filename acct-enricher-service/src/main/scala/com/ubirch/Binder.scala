package com.ubirch

import com.ubirch.models.postgres.{ DefaultPostgresEventDAO, DefaultPostgresFlywaySupport, DefaultPostgresIdentityDAO, DefaultPostgresQuillJdbcContext, DefaultPostgresTenantDAO, EventDAO, FlywaySupport, IdentityDAO, QuillJdbcContext, TenantDAO }
import com.ubirch.services.{ AcctEventsService, DefaultAcctEventsService }
import com.ubirch.services.cluster.{ ClusterService, ConnectionService, DefaultClusterService, DefaultConnectionService }
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.externals.{ DefaultHttpClient, DefaultThingAPI, HttpClient, ThingAPI }
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.lifeCycle.{ DefaultJVMHook, DefaultLifecycle, JVMHook, Lifecycle }

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.{ AbstractModule, Module, TypeLiteral }
import com.typesafe.config.Config
import io.getquill.PostgresDialect
import monix.execution.Scheduler
import org.json4s.Formats

import scala.concurrent.ExecutionContext

/**
  * Represents the default binder for the system components
  */
class Binder
  extends AbstractModule {

  def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(classOf[ConfigProvider])
  def ExecutionContext: ScopedBindingBuilder = bind(classOf[ExecutionContext]).toProvider(classOf[ExecutionProvider])
  def Scheduler: ScopedBindingBuilder = bind(classOf[Scheduler]).toProvider(classOf[SchedulerProvider])
  def Formats: ScopedBindingBuilder = bind(classOf[Formats]).toProvider(classOf[JsonFormatsProvider])
  def HttpClient: ScopedBindingBuilder = bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
  def Lifecycle: ScopedBindingBuilder = bind(classOf[Lifecycle]).to(classOf[DefaultLifecycle])
  def JVMHook: ScopedBindingBuilder = bind(classOf[JVMHook]).to(classOf[DefaultJVMHook])
  def JsonConverterService: ScopedBindingBuilder = bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
  def ClusterService: ScopedBindingBuilder = bind(classOf[ClusterService]).to(classOf[DefaultClusterService])
  def ConnectionService: ScopedBindingBuilder = bind(classOf[ConnectionService]).to(classOf[DefaultConnectionService])
  def QuillJdbcContext: ScopedBindingBuilder = bind(new TypeLiteral[QuillJdbcContext[PostgresDialect]]() {}).to(classOf[DefaultPostgresQuillJdbcContext])
  def FlywaySupport: ScopedBindingBuilder = bind(classOf[FlywaySupport]).to(classOf[DefaultPostgresFlywaySupport])
  def ThingAPI: ScopedBindingBuilder = bind(classOf[ThingAPI]).to(classOf[DefaultThingAPI])
  def TenantDAO: ScopedBindingBuilder = bind(classOf[TenantDAO]).to(classOf[DefaultPostgresTenantDAO])
  def IdentityDAO: ScopedBindingBuilder = bind(classOf[IdentityDAO]).to(classOf[DefaultPostgresIdentityDAO])
  def EventDAO: ScopedBindingBuilder = bind(classOf[EventDAO]).to(classOf[DefaultPostgresEventDAO])
  def AcctEventsService: ScopedBindingBuilder = bind(classOf[AcctEventsService]).to(classOf[DefaultAcctEventsService])

  override def configure(): Unit = {
    Config
    ExecutionContext
    Scheduler
    Formats
    HttpClient
    Lifecycle
    JVMHook
    JsonConverterService
    ClusterService
    ConnectionService
    QuillJdbcContext
    FlywaySupport
    ThingAPI
    TenantDAO
    IdentityDAO
    EventDAO
    AcctEventsService
    ()
  }

}

object Binder {
  def modules: List[Module] = List(new Binder)
}
