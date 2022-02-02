package com.ubirch.models

import java.util.{ Date, UUID }

case class AcctEventRow(
    id: UUID,
    ownerId: UUID,
    identityId: UUID,
    category: String,
    subCategory: String,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    occurredAt: Date
)
