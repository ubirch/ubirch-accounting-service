package com.ubirch

import com.google.inject.TypeLiteral
import com.google.inject.binder.ScopedBindingBuilder
import com.ubirch.models.postgres.{ DefaultEventDAO, DefaultH2FlywaySupport, DefaultH2QuillJdbcContext, DefaultH2IdentityDAO, DefaultH2JobDAO, DefaultH2TenantDAO, EventDAO, FlywaySupport, IdentityDAO, JobDAO, QuillJdbcContext, TenantDAO }
import io.getquill.H2Dialect

class InjectorHelperImpl() extends InjectorHelper(List(new Binder {
  override def JobDAO: ScopedBindingBuilder = bind(classOf[JobDAO]).to(classOf[DefaultH2JobDAO])

  override def TenantDAO: ScopedBindingBuilder = bind(classOf[TenantDAO]).to(classOf[DefaultH2TenantDAO])

  override def IdentityDAO: ScopedBindingBuilder = bind(classOf[IdentityDAO]).to(classOf[DefaultH2IdentityDAO])

  override def EventDAO: ScopedBindingBuilder = bind(classOf[EventDAO]).to(classOf[DefaultEventDAO])

  override def QuillJdbcContext: ScopedBindingBuilder = bind(new TypeLiteral[QuillJdbcContext[H2Dialect]]() {}).to(classOf[DefaultH2QuillJdbcContext])

  override def FlywaySupport: ScopedBindingBuilder = bind(classOf[FlywaySupport]).to(classOf[DefaultH2FlywaySupport])

  override def configure(): Unit = {
    super.configure()
  }
}))
