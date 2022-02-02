package com.ubirch.models

import java.util.{ Date, UUID }

case class AcctEvent(
    id: UUID,
    ownerId: UUID,
    identityId: UUID,
    category: String,
    subCategory: String,
    occurredAt: Date
) {
  //TODO: CHECK WHAT IS VALID
  def validate: Boolean = category.nonEmpty
}
