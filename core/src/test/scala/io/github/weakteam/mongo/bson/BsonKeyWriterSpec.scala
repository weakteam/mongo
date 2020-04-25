package io.github.weakteam.mongo.bson

import io.github.weakteam.mongo.bson.BsonKeyWriterSpec._
import io.github.weakteam.mongo.bson.BsonKeyWriter.KeyWriterSyntax._
import io.github.weakteam.mongo.bson.BsonValue.BsonString
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BsonKeyWriterSpec extends AnyWordSpec with Matchers {

  "BsonKeyWriter#writeKey" should {

    "return valid value for string" in {
      stringBsonKeyWriter.writeKey("weak") mustBe "weak"
    }

    "return valid value for int" in {
      intBsonKeyWriter.writeKey(42) mustBe "42"
    }
  }

  "BsonKeyWriterSyntax#writeKey" should {

    "return valid value for string" in {
      implicit val ins: BsonKeyWriter[String] = stringBsonKeyWriter
      implicit val ins1: BsonWriter[String]   = BsonString(_)
      ("weak" := "henlo") mustBe ("weak" -> BsonString("henlo"))
    }

    "return valid value for int" in {
      implicit val ins: BsonKeyWriter[Int]  = intBsonKeyWriter
      implicit val ins1: BsonWriter[String] = BsonString(_)
      (42 := "henlo") mustBe ("42" -> BsonString("henlo"))
    }
  }
}

object BsonKeyWriterSpec {
  val stringBsonKeyWriter: BsonKeyWriter[String] = identity(_)
  val intBsonKeyWriter: BsonKeyWriter[Int]       = _.toString
}
