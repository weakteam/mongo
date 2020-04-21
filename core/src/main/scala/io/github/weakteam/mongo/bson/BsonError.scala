package io.github.weakteam.mongo.bson

import io.github.weakteam.mongo.bson.BsonPath.Empty

sealed trait BsonError extends Product with Serializable {
  def path: BsonPath = Empty
}

object BsonError {
  @deprecated
  final case object FlakyError extends BsonError
  trait UserDefinedError extends BsonError
  trait ExternalDefinedError extends BsonError
}
