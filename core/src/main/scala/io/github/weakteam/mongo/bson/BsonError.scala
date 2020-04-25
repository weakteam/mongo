package io.github.weakteam.mongo.bson

sealed trait BsonError extends Product with Serializable

object BsonError {
  final case object PathMismatch extends BsonError

  final case class RangeError[A](min: A, max: A) extends BsonError

  final case class MultipleMatches(count: Int) extends BsonError

  final case class MinCountError(requested: Int, actual: Int) extends BsonError

  final case class TypeMismatch(requested: String, actual: BsonValue) extends BsonError

  final case class ValidationError(description: String) extends BsonError

  /**
    * For user-defined errors
    */
  trait UserDefinedError extends BsonError

  /**
    * Only for modules
    */
  trait ExternalDefinedError extends BsonError
}
