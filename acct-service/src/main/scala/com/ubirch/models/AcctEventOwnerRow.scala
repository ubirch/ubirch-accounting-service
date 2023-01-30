package com.ubirch.models

import java.time.Instant
import java.util.UUID

case class AcctEventOwnerRow(
    ownerId: UUID,
    identityId: UUID,
    createdAt: Instant
)
