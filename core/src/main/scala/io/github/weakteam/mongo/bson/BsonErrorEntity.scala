package io.github.weakteam.mongo.bson

final case class BsonErrorEntity(
  at: BsonPath = BsonPath.__,
  path: BsonPath = BsonPath.__,
  error: BsonError
)
