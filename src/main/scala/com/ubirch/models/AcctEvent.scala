package com.ubirch.models

import java.util.{ Date, UUID }

case class AcctEvent(
    id: UUID,
    ownerId: Option[UUID],
    identityId: UUID,
    category: String,
    subCategory: Option[String],
    occurredAt: Date
) {
  def validate: Boolean = category.nonEmpty && subCategory.forall(_.nonEmpty)
}
