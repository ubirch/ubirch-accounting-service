package com.ubirch.models

import java.time.LocalDate
import java.util.UUID

case class AcctEventCountByDayRow(
    identityId: UUID,
    category: String,
    day: LocalDate,
    countEvents: Long
)
