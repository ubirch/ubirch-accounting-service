package com.ubirch.models.cassandra

import java.time.Instant
import java.util.UUID

case class AcctEventOwnerRow(
    ownerId: UUID,
    identityId: UUID,
    createdAt: Instant
)
