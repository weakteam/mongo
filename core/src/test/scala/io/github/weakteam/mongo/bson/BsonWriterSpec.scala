package io.github.weakteam.mongo.bson

import io.github.weakteam.mongo.bson.BsonValue.BsonInt
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.github.weakteam.mongo.implicits._
import cats.syntax.contravariant._

class BsonWriterSpec extends AnyWordSpec with Matchers {
  "BsonWriter" should {
    "return right value" in {
      implicit val intBsonWriter: BsonWriter[Int] = BsonInt(_)

      1.writeBson mustBe BsonInt(1)
    }

    "return right value with contramap" in {
      implicit val intBsonWriter: BsonWriter[Int] = BsonInt(_)
      implicit val anyBsonWriter: BsonWriter[Any] = intBsonWriter.contramap {
        case i: Int => i
        case _      => 0
      }

      1.writeBson mustBe BsonInt(1)
      "".writeBson mustBe BsonInt(0)
      1L.writeBson mustBe BsonInt(0)
    }
  }
}
