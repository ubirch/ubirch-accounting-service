package com.ubirch.models.cassandra

import java.util.{ Date, UUID }

case class AcctEventOwnerRow(
    ownerId: UUID,
    identityId: UUID,
    createdAt: Date
)
