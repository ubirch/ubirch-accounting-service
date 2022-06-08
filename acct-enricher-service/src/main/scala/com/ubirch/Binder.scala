package com.ubirch

import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.jwt._
import com.ubirch.services.lifeCycle.{ DefaultJVMHook, DefaultLifecycle, JVMHook, Lifecycle }

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.{ AbstractModule, Module }
import com.typesafe.config.Config
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
  def Lifecycle: ScopedBindingBuilder = bind(classOf[Lifecycle]).to(classOf[DefaultLifecycle])
  def JVMHook: ScopedBindingBuilder = bind(classOf[JVMHook]).to(classOf[DefaultJVMHook])
  def JsonConverterService: ScopedBindingBuilder = bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
  def TokenCreationService: ScopedBindingBuilder = bind(classOf[TokenCreationService]).to(classOf[DefaultTokenCreationService])
  def TokenVerificationService: ScopedBindingBuilder = bind(classOf[TokenVerificationService]).to(classOf[DefaultTokenVerificationService])
  def PublicKeyDiscoveryService: ScopedBindingBuilder = bind(classOf[PublicKeyDiscoveryService]).to(classOf[DefaultPublicKeyDiscoveryService])
  def PublicKeyPoolService: ScopedBindingBuilder = bind(classOf[PublicKeyPoolService]).to(classOf[DefaultPublicKeyPoolService])

  override def configure(): Unit = {
    Config
    ExecutionContext
    Scheduler
    Formats
    Lifecycle
    JVMHook
    JsonConverterService
    TokenVerificationService
    PublicKeyDiscoveryService
    PublicKeyPoolService
    ()
  }

}

object Binder {
  def modules: List[Module] = List(new Binder)
}
