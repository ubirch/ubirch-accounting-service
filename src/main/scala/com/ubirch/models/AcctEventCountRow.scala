package com.ubirch.models

import java.time.LocalDate
import java.util.UUID

case class AcctEventCountRow(
    identityId: UUID,
    category: String,
    day: LocalDate,
    countEvents: Long
)
