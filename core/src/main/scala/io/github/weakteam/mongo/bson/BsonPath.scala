package io.github.weakteam.mongo.bson

import io.github.weakteam.mongo.bson.BsonError.{PathMismatch, TypeMismatch}
import io.github.weakteam.mongo.bson.BsonReaderResult.Failure
import io.github.weakteam.mongo.bson.BsonValue.BsonArray

import scala.reflect.ClassTag
import scala.util.matching.Regex

sealed trait BsonPath extends Product with Serializable {
  def parent: Option[BsonPath]
}

object BsonPath {
  final case class IdBsonPath(id: Int, parent: Option[BsonPath]) extends BsonPath { self =>
//    def readInPlace[A: BsonReader]: BsonReader[A] = {
//      case BsonValue.BsonArray(value) => value.
//      case rest => Failure(TypeMismatch[BsonArray](implicitly[ClassTag[BsonArray]].runtimeClass, rest, self, parent))
//    }
  }
  final case class KeyBsonPath(key: String, parent: Option[BsonPath]) extends BsonPath
  final case class RegexBsonPath(regex: Regex, parent: Option[BsonPath]) extends BsonPath
  final case class RecursiveKeyBsonPath(key: String, parent: Option[BsonPath]) extends BsonPath
  final case class RecursiveRegexBsonPath(regex: Regex, parent: Option[BsonPath]) extends BsonPath
  final case class EmptyBsonPath(parent: Option[BsonPath] = None) extends BsonPath
}
