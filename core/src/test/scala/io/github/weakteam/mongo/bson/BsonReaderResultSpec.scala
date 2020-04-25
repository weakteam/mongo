package io.github.weakteam.mongo.bson

import cats.data.{Ior, NonEmptyList}
import cats.syntax.applicative._
import cats.syntax.functor._
import io.github.weakteam.mongo.bson.BsonError.TypeMismatch
import io.github.weakteam.mongo.bson.BsonReaderResult._
import io.github.weakteam.mongo.bson.BsonReaderResultSpec._
import io.github.weakteam.mongo.bson.BsonReader._
import io.github.weakteam.mongo.bson.BsonValue._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BsonReaderResultSpec extends AnyWordSpec with Matchers {
  "BsonReaderResult#toIor" should {
    "return right" in {
      bsonStringReader.readBson(BsonString("foo")).toIor mustBe Ior.right(BsonString("foo"))
    }

    "return left" in {
      bsonIntReader.readBson(BsonString("foo")).toIor mustBe Ior.left(
        NonEmptyList.of(BsonErrorEntity(TypeMismatch("BsonInt", BsonString("foo"))))
      )
    }

    "return both" in {
      bsonListIntReader.readBson(BsonArray(BsonInt(4), BsonString("bar"))).toIor mustBe Ior.both(
        NonEmptyList.of(BsonErrorEntity(TypeMismatch("BsonInt", BsonString("bar")))),
        List(4)
      )
    }
  }

  "BsonReaderResult#toOption" should {
    "return Some (succ)" in {
      bsonStringReader.readBson(BsonString("foo")).toOption mustBe Some(BsonString("foo"))
    }

    "return None" in {
      bsonIntReader.readBson(BsonString("foo")).toOption mustBe None
    }

    "return Some (partial succ)" in {
      bsonListIntReader.readBson(BsonArray(BsonInt(4), BsonString("bar"))).toOption mustBe Some(List(4))
    }
  }
}

object BsonReaderResultSpec {
  val bsonStringReader: BsonReader[BsonString] = {
    case s: BsonString => Success(value = s)
    case other         => Failure(TypeMismatch("BsonString", other))
  }

  val bsonIntReader: BsonReader[BsonInt] = {
    case s: BsonInt => Success(value = s)
    case other      => Failure(TypeMismatch("BsonInt", other))
  }

  val intReader: BsonReader[Int] = bsonIntReader.map(_.value)

  val bsonIntApReader: BsonReader[BsonInt => Int] = {
    ((_: BsonInt).value * 2).pure[BsonReader]
  }

  val bsonListIntReader: BsonReader[List[Int]] = {
    case BsonArray(list) =>
      list.foldLeft((List.empty[BsonErrorEntity], BsonPath.__, List.empty[Int])) {
        case ((err, path, suc), v) =>
          intReader.readBson(v) match {
            case Success(value, path)                => (err, path, value :: suc)
            case Failure(errors)                     => (errors.toList ::: err, path, suc)
            case PartialSuccess(errors, path, value) => (errors.toList ::: err, path, value :: suc)
          }
      } match {
        case (head :: rest, path, succ @ _ :: _) => PartialSuccess(NonEmptyList(head, rest), path, succ)
        case (head :: rest, _, _)                => Failure(NonEmptyList(head, rest))
        case (Nil, path, succ)                   => Success(succ, path)
      }
    case other => Failure(TypeMismatch("BsonArray", other))
  }

  val bson = BsonDocument(
    "string" -> BsonString("foo"),
    "int" -> BsonInt(3),
    "array" -> BsonArray(BsonInt(4), BsonString("bar"))
  )
}
