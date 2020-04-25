package io.github.weakteam.mongo.bson

import cats.data.NonEmptyList
import cats.syntax.apply._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import cats.syntax.semigroupal._
import io.github.weakteam.mongo.bson.BsonError.TypeMismatch
import io.github.weakteam.mongo.bson.BsonPathNode.KeyBsonPathNode
import io.github.weakteam.mongo.bson.BsonReaderResult.{Failure, Success}
import io.github.weakteam.mongo.bson.BsonValue.{BsonDocument, BsonInt, BsonNull, BsonString}
import io.github.weakteam.mongo.bson.BsonReaderSpec._
import io.github.weakteam.mongo.bson.BsonPath._
import io.github.weakteam.mongo.bson.BsonReader._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BsonReaderSpec extends AnyWordSpec with Matchers {
  "BsonReader#bsonApplicative" should {

    "return success for pure" in {
      2.pure[BsonReader].readBson(BsonNull) mustBe Success(value = 2)
    }

    "return success for ap" in {
      bsonIntApReader.ap(bsonIntReader).readBson(BsonInt(2)) mustBe Success(value = 4)
    }

    "return success for mapped reader" in {
      bsonIntReader.map(_.value).readBson(BsonInt(2)) mustBe Success(value = 2)
    }

    "return appropriate product" in {
      bsonIntReader.product(bsonStringReader).readBson(BsonString("bson")) mustBe Failure(
        TypeMismatch("BsonInt", BsonString("bson"))
      )
      bsonStringReader.product(bsonIntReader).readBson(BsonString("bson")) mustBe Failure(
        TypeMismatch("BsonInt", BsonString("bson"))
      )
      bsonStringReader.product(bsonStringReader).readBson(BsonString("bson")) mustBe Success(value =
        (BsonString("bson"), BsonString("bson"))
      )
    }

    "return error" in {
      NonEmptyList
        .of(BsonErrorEntity(TypeMismatch("BsonInt", BsonString("bson"))))
        .raiseError[BsonReader, Int]
        .readBson(BsonNull) mustBe Failure(TypeMismatch("BsonInt", BsonString("bson")))
    }

    "successful recovering" in {
      NonEmptyList
        .of(BsonErrorEntity(TypeMismatch("BsonInt", BsonString("bson"))))
        .raiseError[BsonReader, Int]
        .handleError(_ => 1)
        .readBson(BsonNull) mustBe Success(value = 1)

      NonEmptyList
        .of(BsonErrorEntity(TypeMismatch("BsonInt", BsonString("bson"))))
        .raiseError[BsonReader, Int]
        .handleErrorWith(_ => 1.pure)
        .readBson(BsonNull) mustBe Success(value = 1)
    }

    "successful combined" in {
      (
        (__ \ "string").read(bsonStringReader).map(_.value),
        (__ \ "int").read(bsonIntReader).map(_.value)
      ).tupled.readBson(bson) mustBe Success(("foo", 3), BsonPath(KeyBsonPathNode("int")))
    }
  }
}

object BsonReaderSpec {

  val bsonStringReader: BsonReader[BsonString] = {
    case s: BsonString => Success(value = s)
    case other         => Failure(TypeMismatch("BsonString", other))
  }

  val bsonIntReader: BsonReader[BsonInt] = {
    case s: BsonInt => Success(value = s)
    case other      => Failure(TypeMismatch("BsonInt", other))
  }

  val bsonIntApReader: BsonReader[BsonInt => Int] = {
    ((_: BsonInt).value * 2).pure[BsonReader]
  }

  val bson = BsonDocument(
    "string" -> BsonString("foo"),
    "int" -> BsonInt(3)
  )
}
