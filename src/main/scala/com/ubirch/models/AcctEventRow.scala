package com.ubirch.models

import java.util.{ Date, UUID }

case class AcctEventRow(
    id: UUID,
    ownerId: UUID,
    identityId: Option[UUID],
    category: String,
    description: Option[String],
    occurredAt: Date,
    createAt: Date
)
