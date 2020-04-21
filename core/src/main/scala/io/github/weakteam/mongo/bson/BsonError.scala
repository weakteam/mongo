package io.github.weakteam.mongo.bson

sealed trait BsonError extends Product with Serializable {
  def at: BsonPath
  def path: Option[BsonPath]
}

object BsonError {
  final case class PathMismatch(at: BsonPath, path: Option[BsonPath]) extends BsonError
  final case class RangeError[A](min: A, max: A, at: BsonPath, path: Option[BsonPath]) extends BsonError
  final case class MultipleMatches(count: Int, at: BsonPath, path: Option[BsonPath]) extends BsonError
  final case class MinCountError(requested: Int, actual: Int, at: BsonPath, path: Option[BsonPath]) extends BsonError
  final case class TypeMismatch[T](requested: Class[T], actual: BsonValue, at: BsonPath, path: Option[BsonPath]) extends BsonError
  final case class ValidationError(description: String, at: BsonPath, path: Option[BsonPath]) extends BsonError

  /**
   * For user-defined errors
   */
  trait UserDefinedError extends BsonError

  /**
   * Only for modules
   */
  trait ExternalDefinedError extends BsonError
}
