package com.ubirch.models

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

case class AcctEventCountByHourRow(
    identityId: UUID,
    category: String,
    day: LocalDate,
    time: LocalTime,
    countEvents: Long
)
