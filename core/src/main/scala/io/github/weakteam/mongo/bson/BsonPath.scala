package io.github.weakteam.mongo.bson

import cats.syntax.either._
import io.github.weakteam.mongo.bson.BsonError._
import io.github.weakteam.mongo.bson.BsonPath._
import io.github.weakteam.mongo.bson.BsonReaderResult.Failure
import io.github.weakteam.mongo.bson.BsonValue.{BsonArray, BsonDocument}

import scala.util.matching.Regex

sealed trait BsonPath extends Product with Serializable { self =>
  def parent: Option[BsonPath]

  protected def readPath(bson: BsonValue): Either[BsonError, BsonValue]

  final def readAt(bson: BsonValue): Either[BsonError, BsonValue] = {
    @scala.annotation.tailrec
    def loop(
      arg: Option[BsonPath] = Some(self),
      acc: List[BsonPath] = Nil,
      bson: BsonValue
    ): Either[BsonError, BsonValue] = {
      arg match {
        case Some(value) => loop(value.parent, value :: acc, bson)
        case _           => acc.foldLeft(bson.asRight[BsonError])((state, path) => state.flatMap(path.readPath))
      }
    }

    loop(bson = bson)
  }

  def read[A](implicit reader: BsonReader[A]): BsonReader[A] = { bson =>
    readAt(bson) match {
      case Left(value)  => Failure(value.withPath(this))
      case Right(value) => reader.readBson(value)
    }
  }

  def \(key: String): KeyBsonPath     = KeyBsonPath(key, Some(self))
  def \?(regex: Regex): RegexBsonPath = RegexBsonPath(regex, Some(self))
}

object BsonPath {

  final case class IdBsonPath(id: Int, parent: Option[BsonPath]) extends BsonPath { self =>
    protected def readPath(bson: BsonValue): Either[BsonError, BsonValue] = {
      bson match {
        case BsonArray(value) =>
          value.drop(id - 1).headOption.toRight(MinCountError(id, value.length, self, parent))
        case other => Left(TypeMismatch("BsonArray", other, self, None))
      }
    }
  }

  final case class KeyBsonPath(key: String, parent: Option[BsonPath]) extends BsonPath { self =>
    protected def readPath(bson: BsonValue): Either[BsonError, BsonValue] = {
      bson match {
        case BsonDocument(value) => value.get(key).toRight(PathMismatch(self, parent))
        case other               => Left(TypeMismatch("BsonDocument", other, self, None))
      }
    }
  }

  final case class RegexBsonPath(regex: Regex, parent: Option[BsonPath]) extends BsonPath { self =>
    protected def readPath(bson: BsonValue): Either[BsonError, BsonValue] = {
      bson match {
        case BsonDocument(value) =>
          value.collect { case (k, v) if regex.pattern.matcher(k).matches() => v }.toList match {
            case head :: Nil   => Right(head)
            case list @ _ :: _ => Left(MultipleMatches(list.length, self, None))
            case _             => Left(ValidationError(s"No matches for $regex regex", self, None))
          }
        case other => Left(TypeMismatch("BsonDocument", other, self, None))
      }
    }
  }

  final case class EmptyBsonPath(parent: Option[BsonPath] = None) extends BsonPath {
    protected def readPath(bson: BsonValue): Either[BsonError, BsonValue] = Right(bson)
  }

  implicit def toStringBsonPath(s: String): KeyBsonPath = KeyBsonPath(s, None)
  implicit def toRegexBsonPath(r: Regex): RegexBsonPath = RegexBsonPath(r, None)

}
