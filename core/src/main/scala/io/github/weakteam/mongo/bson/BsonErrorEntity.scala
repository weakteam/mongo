package io.github.weakteam.mongo.bson

final case class BsonErrorEntity(
  error: BsonError,
  path: BsonPath = BsonPath.__,
  at: BsonPath = BsonPath.__
) {
  def prepath(pre: BsonPath) = copy(path = path ::: pre, at = at ::: pre)
}
