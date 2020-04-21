package io.github.weakteam.mongo.bson

sealed trait BsonPath extends Product with Serializable

object BsonPath {
  final case object Empty extends BsonPath
}
