package io.github.weakteam.mongo.bson

import java.util.regex.Pattern

import io.github.weakteam.mongo.bson.BsonValue._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.github.weakteam.mongo.implicits._
import cats.syntax.contravariant._

import scala.util.matching.Regex

class BsonWriterSpec extends AnyWordSpec with Matchers {
  "BsonWriter" should {
    "return right value" in {
      BsonWriter.instance(BsonInt).writeBson(1) mustBe BsonInt(1)
    }

    "return right value with contramap" in {
      implicit val anyBsonWriter: BsonWriter[Any] = BsonWriter.intWriter.contramap {
        case i: Int => i
        case _      => 0
      }

      1.writeBson mustBe BsonInt(1)
      "".writeBson mustBe BsonInt(0)
      1L.writeBson mustBe BsonInt(0)
    }

    "return right value with manual instance creation" in {
      val intBsonWriter: BsonWriter[Int] = BsonWriter.instance(BsonInt)

      intBsonWriter.writeBson(1) mustBe BsonInt(1)
    }
  }

  "BsonWriter#instances" should {
    "write identity" in {
      BsonWriter[BsonValue].writeBson(BsonString("bson")) mustBe BsonString("bson")
    }

    "write int" in {
      BsonWriter[Int].writeBson(42) mustBe BsonInt(42)
    }

    "write long" in {
      BsonWriter[Long].writeBson(42L) mustBe BsonLong(42L)
    }

    "write boolean" in {
      BsonWriter[Boolean].writeBson(true) mustBe BsonBoolean(true)
    }

    "write double" in {
      BsonWriter[Double].writeBson(42.52d) mustBe BsonFloat(42.52d)
    }

    "write float" in {
      BsonWriter[Float].writeBson(42.52f) mustBe BsonFloat(42.52f)
    }

    "write regex" in {
      BsonWriter[Regex].writeBson("foo".r) mustBe BsonRegex("foo")
    }

    "write pattern" in {
      BsonWriter[Pattern].writeBson("foo".r.pattern) mustBe BsonRegex("foo")
    }
  }

  "BsonWriter#wrapped-instances" should {
    implicit def implConv[A: BsonWriter](value: A): BsonValue = value.writeBson

    "write option" in {
      BsonWriter[Option[Int]].writeBson(Some(4)) mustBe BsonInt(4)
      BsonWriter[Option[Int]].writeBson(None) mustBe BsonNull
    }

    "write either" in {
      BsonWriter[Either[Boolean, String]].writeBson(Right("foo")) mustBe BsonString("foo")
      BsonWriter[Either[Boolean, String]].writeBson(Left(true)) mustBe BsonBoolean(true)
    }

    "write list" in {
      BsonWriter[List[Int]].writeBson(List(1, 4, 5)) mustBe BsonArray(1, 4, 5)
    }

    "write set" in {
      BsonWriter[Set[String]].writeBson(Set("foo", "foo", "bar")) mustBe BsonArray("foo", "bar")
    }

    "write map" in {
      implicit val stringBsonKeyWriter: BsonKeyWriter[String] = identity(_)

      BsonWriter[Map[String, Int]]
        .writeBson(Map("foo" -> 3, "foo" -> 4, "bar" -> 5)) mustBe BsonDocument("foo" := 4, "bar" := 5)
    }

//    "write Enum" in {
//      def wr: BsonWriter[TestEnum.Value] = TestEnum
//      wr.writeBson(TestEnum.A) mustBe BsonBinary(UserDefinedSubtype(0x05), 1.toString.getBytes("UTF-8"))
//
//      wr.writeBson(TestEnum.B) mustBe BsonBinary(UserDefinedSubtype(0x05), 2.toString.getBytes("UTF-8"))
//    }
  }
}

//object BsonWriterSpec {
//  object TestEnum extends Enumeration {
//    val A, B = Value
//  }
//}
