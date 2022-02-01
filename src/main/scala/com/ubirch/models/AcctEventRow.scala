package com.ubirch.models

import java.time.{ Instant, LocalDate, LocalDateTime, ZoneOffset }
import java.util.UUID

case class AcctEventRow(
    id: UUID,
    ownerId: UUID,
    identityId: UUID,
    category: String,
    description: Option[String],
    day: LocalDate,
    occurredAt: Instant,
    createdAt: Instant
) {

  val localDateTime: LocalDateTime = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC)
  val hour = localDateTime.toLocalTime.withMinute(0).withSecond(0).withNano(0)

}

object AcctEventRow {
  def apply(id: UUID, ownerId: UUID, identityId: UUID, category: String, description: Option[String], occurredAt: Instant): AcctEventRow = {
    new AcctEventRow(
      id,
      ownerId,
      identityId,
      category,
      description,
      LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC).toLocalDate, occurredAt, Instant.now()
    )
  }
}
