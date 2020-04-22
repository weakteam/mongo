package io.github.weakteam.mongo.bson

sealed trait BsonError extends Product with Serializable {
  def at: BsonPath
  def path: Option[BsonPath]

  def withPath(newPath: BsonPath): BsonError
}

object BsonError {
  final case class PathMismatch(at: BsonPath, path: Option[BsonPath] = None) extends BsonError {
    def withPath(newPath: BsonPath): PathMismatch = copy(path = Some(newPath))
  }

  final case class RangeError[A](min: A, max: A, at: BsonPath, path: Option[BsonPath] = None) extends BsonError {
    def withPath(newPath: BsonPath): RangeError[A] = copy(path = Some(newPath))
  }

  final case class MultipleMatches(count: Int, at: BsonPath, path: Option[BsonPath] = None) extends BsonError {
    def withPath(newPath: BsonPath): MultipleMatches = copy(path = Some(newPath))
  }

  final case class MinCountError(requested: Int, actual: Int, at: BsonPath, path: Option[BsonPath] = None)
    extends BsonError {
    def withPath(newPath: BsonPath): MinCountError = copy(path = Some(newPath))
  }

  final case class TypeMismatch(requested: String, actual: BsonValue, at: BsonPath, path: Option[BsonPath] = None)
    extends BsonError {
    def withPath(newPath: BsonPath): TypeMismatch = copy(path = Some(newPath))
  }

  final case class ValidationError(description: String, at: BsonPath, path: Option[BsonPath] = None) extends BsonError {
    def withPath(newPath: BsonPath): ValidationError = copy(path = Some(newPath))
  }

  /**
    * For user-defined errors
    */
  trait UserDefinedError extends BsonError

  /**
    * Only for modules
    */
  trait ExternalDefinedError extends BsonError
}
