package com.ubirch.models

import java.util.{ Date, UUID }

case class AcctEventOwnerRow(
    ownerId: UUID,
    identityId: UUID,
    createdAt: Date
)
