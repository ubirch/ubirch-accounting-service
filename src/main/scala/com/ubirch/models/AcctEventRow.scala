package com.ubirch.models

import java.util.{ Date, UUID }

case class AcctEventRow(id: UUID, ownerId: UUID, identityId: UUID, category: String, description: Option[String], tokenValue: Option[String], day: Date, occurredAt: Date, createdAt: Date)
