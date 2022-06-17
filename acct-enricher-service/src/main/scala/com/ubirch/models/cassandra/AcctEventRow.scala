package com.ubirch.models.cassandra

import java.util.{ Date, UUID }

case class AcctEventRow(
    id: UUID,
    identityId: UUID,
    category: String,
    subCategory: String,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    occurredAt: Date,
    externalId: Option[String]
)
