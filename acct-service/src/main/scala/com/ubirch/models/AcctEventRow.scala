package com.ubirch.models

import java.time.Instant
import java.util.UUID

case class AcctEventRow(
    id: UUID,
    identityId: UUID,
    category: String,
    subCategory: String,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    occurredAt: Instant,
    externalId: Option[String]
)
