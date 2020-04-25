package io.github.weakteam.mongo.bson

import io.github.weakteam.mongo.bson.BsonError._
import io.github.weakteam.mongo.bson.BsonValue.{BsonArray, BsonDocument}

import scala.util.matching.Regex

sealed trait BsonPathNode extends Product with Serializable { self =>

  private[bson] def readPath: BsonValue => Either[BsonError, (BsonPath, BsonValue)]

  def toPath: BsonPath = BsonPath(self)

}

object BsonPathNode {

  final case class IdBsonPathNode(id: Int) extends BsonPathNode {
    def readPath = {
      case BsonArray(value) =>
        value.drop(id - 1).headOption.map((toPath, _)).toRight(MinCountError(id, value.length))
      case other => Left(TypeMismatch("BsonArray", other))
    }
  }

  final case class KeyBsonPathNode(key: String) extends BsonPathNode {
    def readPath = {
      case BsonDocument(value) => value.get(key).map((toPath, _)).toRight(PathMismatch)
      case other               => Left(TypeMismatch("BsonDocument", other))
    }
  }

  final case class RegexBsonPathNode(regex: Regex) extends BsonPathNode {
    def readPath = {
      case BsonDocument(value) =>
        value.collect { case (k, v) if k.matches(regex.regex) => v }.toList match {
          case head :: Nil   => Right((toPath, head))
          case list @ _ :: _ => Left(MultipleMatches(list.length))
          case _             => Left(ValidationError(s"No matches for $regex"))
        }
      case other => Left(TypeMismatch("BsonDocument", other))
    }
  }

  final case object RootBsonPathNode extends BsonPathNode {
    def readPath = bson => Right((toPath, bson))
  }

  private[this] def mapLookup(bson: BsonValue, f: String => Boolean): Option[(BsonPath, BsonValue)] = {

    def loop(bson: BsonValue, paths: BsonPath = BsonPath.__): Option[(BsonPath, BsonValue)] = {
      bson match {
        case BsonDocument(value) =>
          val (sub, notsub) = value.toList.partition { case (k, _) => f(k) }
          sub match {
            case (key, head) :: Nil => Some((KeyBsonPathNode(key) :: paths, head))
            case _ =>
              notsub match {
                case Nil => None
                case _ =>
                  notsub.foldLeft(List.empty[(BsonPath, BsonValue)]) {
                    case (acc, (key, bson)) =>
                      loop(bson, KeyBsonPathNode(key) :: paths).fold(acc)(_ :: acc)
                  } match {
                    case head :: Nil => Some(head)
                    case _           => None
                  }
              }
          }
        case _ => None
      }
    }

    loop(bson)
  }

  final case class RecursiveKeyBsonPathNode(key: String) extends BsonPathNode { self =>
    def readPath = mapLookup(_, _ == key).toRight(PathMismatch)
  }

  final case class RecursiveRegexBsonPathNode(regex: Regex) extends BsonPathNode { self =>
    def readPath = mapLookup(_, _.matches(regex.regex)).toRight(PathMismatch)
  }
}
