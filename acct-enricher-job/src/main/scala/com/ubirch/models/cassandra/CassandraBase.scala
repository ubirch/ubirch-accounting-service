package com.ubirch.models.cassandra

import io.getquill.context.cassandra.CassandraContext
import io.getquill.context.cassandra.encoding.{ Decoders, Encoders }

/**
  * Value that represents a pointer to a Cassandra Table.
  * Very useful for mapping different versions for a particular table.
  * @tparam T represents the scala value that represent the database table.
  */

trait CassandraBase[T] {

  val db: CassandraContext[_] with Encoders with Decoders
  import db._

  implicit val pointingAt: SchemaMeta[T]

}
