package io.github.weakteam.mongo.bson

import java.util.regex.Pattern

import cats.data.NonEmptyList
import cats.syntax.apply._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import cats.syntax.semigroupal._
import io.github.weakteam.mongo.bson.BsonError.TypeMismatch
import io.github.weakteam.mongo.bson.BsonPathNode.KeyBsonPathNode
import io.github.weakteam.mongo.bson.BsonReaderResult.{Failure, Success}
import io.github.weakteam.mongo.bson.BsonReaderResult.bsonReaderResultApplicativeErrorInstance
import io.github.weakteam.mongo.bson.BsonValue._
import io.github.weakteam.mongo.bson.BsonReaderSpec._
import io.github.weakteam.mongo.bson.BsonPath._
import io.github.weakteam.mongo.bson.BsonReader._
import io.github.weakteam.mongo.bson.BsonWriter._
import io.github.weakteam.mongo.implicits._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.matching.Regex

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
        .handleErrorWith(_ => 1.pure[BsonReader])
        .readBson(BsonNull) mustBe Success(value = 1)
    }

    "successful combined" in {
      (
        (__ \ "string").read(bsonStringReader).map(_.value),
        (__ \ "int").read(bsonIntReader).map(_.value)
      ).tupled.readBson(bson) mustBe Success(("foo", 3), BsonPath(KeyBsonPathNode("int")))
    }
  }

  "BsonReader#instances" should {
    "successful read identity" in {
      BsonReader[BsonValue].readBson(BsonString("foo")) mustBe Success(BsonString("foo"))
    }

    "successful read Int" in {
      BsonReader[Int].readBson(bsonInt) mustBe Success(bsonIntValue)
    }

    "unsuccessful read Int" in {
      (allBson - bsonInt).forall(BsonReader[Int].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Long" in {
      BsonReader[Long].readBson(bsonLong) mustBe Success(bsonLongValue)
    }

    "unsuccessful read Long" in {
      (allBson - bsonLong).forall(BsonReader[Long].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Boolean" in {
      BsonReader[Boolean].readBson(bsonBoolean) mustBe Success(bsonBooleanValue)
    }

    "unsuccessful read Boolean" in {
      (allBson - bsonBoolean).forall(BsonReader[Boolean].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Float" in {
      BsonReader[Float].readBson(bsonFloat) mustBe Success(bsonFloatValue.toFloat)
    }

    "unsuccessful read Float" in {
      (allBson - bsonFloat).forall(BsonReader[Float].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Double" in {
      BsonReader[Double].readBson(bsonFloat) mustBe Success(bsonFloatValue)
    }

    "unsuccessful read Double" in {
      (allBson - bsonFloat).forall(BsonReader[Double].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Regex" in {
      BsonReader[Regex].readBson(bsonRegex).map(_.regex) mustBe Success(bsonRegexValue.regex)
    }

    "unsuccessful read Regex" in {
      (allBson - bsonRegex - bsonPattern).forall(BsonReader[Regex].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Pattern" in {
      BsonReader[Pattern].readBson(bsonRegex).map(_.pattern()) mustBe Success(bsonPatternValue.pattern())
    }

    "unsuccessful read Pattern" in {
      (allBson - bsonRegex - bsonPattern).forall(BsonReader[Pattern].readBson(_).toOption.isEmpty) mustBe true
    }
  }

  "BsonReader#wrapped-instances" should {
    "successful read option" in {
      BsonReader[Option[Int]].readBson(bsonInt) mustBe Success(Some(bsonIntValue))
      BsonReader[Option[Int]].readBson(BsonNull) mustBe Success(None)
    }

    "unsuccessful read option" in {
      (allBson - bsonInt - BsonNull).forall(BsonReader[Option[Int]].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Either" in {
      BsonReader[Either[String, Int]].readBson(bsonInt) mustBe Success(Right(bsonIntValue))
      BsonReader[Either[String, Int]].readBson(bsonString) mustBe Success(Left(bsonStringValue))
    }

    "unsuccessful read Either" in {
      (allBson - bsonInt - bsonString).forall(BsonReader[Either[String, Int]].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read List" in {
      BsonReader[List[Int]].readBson(bsonIntArray) mustBe Success(bsonIntArrayValue)
      BsonReader[List[Int]].readBson(bsonPartialNonIntArray).toIor.right mustBe Some(bsonIntArrayValue)
      BsonReader[List[Int]].readBson(bsonPartialNonIntArray).toIor.isBoth mustBe true
    }

    "unsuccessful read List" in {
      (allBson - bsonIntArray - bsonPartialNonIntArray)
        .forall(BsonReader[List[Int]].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Set" in {
      BsonReader[Set[Int]].readBson(bsonIntArray) mustBe Success(bsonIntSetValue)
      BsonReader[Set[Int]].readBson(bsonPartialNonIntArray).toIor.right mustBe Some(bsonIntSetValue)
      BsonReader[Set[Int]].readBson(bsonPartialNonIntArray).toIor.isBoth mustBe true
    }

    "unsuccessful read Set" in {
      (allBson - bsonIntArray - bsonPartialNonIntArray)
        .forall(BsonReader[Set[Int]].readBson(_).toOption.isEmpty) mustBe true
    }

    "successful read Map" in {
      BsonReader[Map[String, Int]].readBson(bsonIntDocument) mustBe Success(bsonIntDocumentValue)
      BsonReader[Map[String, Int]].readBson(bsonPartialNonIntDocument).toIor.right mustBe Some(bsonIntDocumentValue)
      BsonReader[Map[String, Int]].readBson(bsonPartialNonIntDocument).toIor.isBoth mustBe true
    }

    "unsuccessful read Map" in {
      (allBson - bsonIntDocument - bsonPartialNonIntDocument)
        .forall(BsonReader[Map[String, Int]].readBson(_).toOption.isEmpty) mustBe true
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

  val bsonIntValue = 4
  val bsonInt      = BsonInt(bsonIntValue)

  val bsonLongValue = 4L
  val bsonLong      = BsonLong(bsonLongValue)

  val bsonStringValue = "string"
  val bsonString      = BsonString(bsonStringValue)

  val bsonBooleanValue = true
  val bsonBoolean      = BsonBoolean(bsonBooleanValue)

  val bsonFloatValue = 3.4d
  val bsonFloat      = BsonFloat(bsonFloatValue)

  val bsonRegexValue = "".r
  val bsonRegex      = BsonRegex(bsonRegexValue.regex)

  val bsonPatternValue = "".r.pattern
  val bsonPattern      = BsonRegex(bsonPatternValue.pattern())

  val bsonIntArrayValue      = List(bsonIntValue, bsonIntValue, bsonIntValue)
  val bsonIntSetValue        = bsonIntArrayValue.toSet
  val bsonIntArray           = BsonArray(bsonInt, bsonInt, bsonInt)
  val bsonPartialNonIntArray = BsonArray(bsonInt, bsonInt, bsonInt, bsonString)
  val bsonNonIntArray        = BsonArray(bsonString, bsonString)

  implicit val stringKeyWriter: BsonKeyWriter[String] = identity(_)

  val bsonIntDocumentValue      = Map("1" -> 4, "2" -> 5, "3" -> 6)
  val bsonIntDocument           = BsonDocument("1" := 4, "2" := 5, "3" := 6)
  val bsonPartialNonIntDocument = BsonDocument("0" := "4", "1" := 4, "2" := 5, "3" := 6)
  val bsonNonIntDocument        = BsonDocument("1" := "4", "2" := "5", "3" := "6")

  val allBson = Set(
    bsonInt,
    bsonLong,
    bsonBoolean,
    bsonRegex,
    bsonString,
    bsonFloat,
    bsonPattern,
    bsonIntArray,
    bsonPartialNonIntArray,
    bsonNonIntArray,
    bsonIntDocument,
    bsonPartialNonIntArray,
    bsonNonIntDocument,
    BsonNull,
    BsonMinKey,
    BsonMaxKey,
    BsonJS(""),
    BsonSymbol(""),
    BsonTimestamp(1, 1),
    BsonPointer(Array()),
    BsonDecimal(BigDecimal(3)),
    BsonDate(1),
    BsonBinary(BsonSubtype.GenericSubtype, Array())
  )

  val bson = BsonDocument(
    "string" -> BsonString("foo"),
    "int" -> BsonInt(3)
  )
}
