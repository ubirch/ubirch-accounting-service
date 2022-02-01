package com.ubirch.models

import io.getquill.SnakeCase
import io.getquill.context.cassandra.CassandraContext
import io.getquill.context.cassandra.encoding.{ Decoders, Encoders }

import java.time.LocalDate

/**
  * Value that represents a pointer to a Cassandra Table.
  * Very useful for mapping different versions for a particular table.
  * @tparam T represents the scala value that represent the database table.
  */

trait CassandraBase[T] {

  val db: CassandraContext[SnakeCase] with Encoders with Decoders

  import db._

  implicit val pointingAt: SchemaMeta[T]

  implicit class DateQuotes(left: LocalDate) {
    def >=(right: LocalDate) = quote(infix"$left >= $right".as[Boolean])
    def <=(right: LocalDate) = quote(infix"$left <= $right".as[Boolean])
  }

}
