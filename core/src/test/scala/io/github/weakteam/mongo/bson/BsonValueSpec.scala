package io.github.weakteam.mongo.bson

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cats.syntax.show._
import BsonValue._

class BsonValueSpec extends AnyWordSpec with Matchers {
  "BsonValueShow" should {

    "return appropriate value for Null" in {
      show"$BsonNull" mustBe "BsonNull"
    }

    "return appropriate value for Undefined" in {
      show"$BsonUndefined" mustBe "BsonUndefined"
    }

    "return appropriate value for BsonMinKey" in {
      show"$BsonMinKey" mustBe "BsonMinKey"
    }

    "return appropriate value for BsonMaxKey" in {
      show"$BsonMaxKey" mustBe "BsonMaxKey"
    }

    "return appropriate value for BsonString" in {
      show"${BsonString("test")}" mustBe "BsonString(value = test)"
    }

    "return appropriate value for BsonRegex" in {
      show"${BsonRegex("test")}" mustBe "BsonRegex(value = test)"
    }

    "return appropriate value for BsonJS" in {
      show"${BsonJS("test")}" mustBe "BsonJS(value = test)"
    }

    "return appropriate value for BsonSymbol" in {
      show"${BsonSymbol("test")}" mustBe "BsonSymbol(value = test)"
    }

    "return appropriate value for BsonObjectId" in {
      show"${BsonObjectId("test")}" mustBe "BsonObjectId(value = test)"
    }

    "return appropriate value for BsonInt" in {
      show"${BsonInt(42)}" mustBe "BsonInt(value = 42)"
    }

    "return appropriate value for BsonLong" in {
      show"${BsonLong(42L)}" mustBe "BsonLong(value = 42)"
    }

    "return appropriate value for BsonFloat" in {
      show"${BsonFloat(42.42)}" mustBe "BsonFloat(value = 42.42)"
    }

    "return appropriate value for BsonBoolean" in {
      show"${BsonBoolean(true)}" mustBe "BsonBoolean(value = true)"
      show"${BsonBoolean(false)}" mustBe "BsonBoolean(value = false)"
    }

    "return appropriate value for BsonTimestamp" in {
      show"${BsonTimestamp(43, 42)}" mustBe "BsonTimestamp(counter = 43, value = 42)"
    }

    "return appropriate value for BsonPointer" in {
      show"${BsonPointer(Array(1.toByte, 2.toByte))}" mustBe "BsonPointer(value = [1, 2])"
    }

    "return appropriate value for BsonDecimal" in {
      show"${BsonDecimal(42)}" mustBe "BsonDecimal(value = 42)"
    }

    "return appropriate value for BsonDate" in {
      show"${BsonDate(424242L)}" mustBe "BsonDate(value = 424242)"
    }

    "return appropriate value for BsonBinary" in {
      show"${BsonBinary(BsonSubtype.BinarySubtype, Array(0x1, 0x2))}" mustBe "BsonBinary(type = BsonBinary, value = [1, 2])"
    }

    "return appropriate value for BsonArray" in {
      val bson = BsonArray(BsonString("test"), BsonDocument("test" -> BsonInt(42)))
      show"$bson" mustBe "BsonArray(values = [BsonString(value = test), BsonDocument(values = {(key = test, value = BsonInt(value = 42))})])"
    }

    "return appropriate value for BsonDocument" in {
      val bson = BsonDocument("test" -> BsonInt(42), "list" -> BsonArray(BsonString("test")))
      show"$bson" mustBe "BsonDocument(values = {" +
        "(key = test, value = BsonInt(value = 42)), " +
        "(key = list, value = BsonArray(values = [BsonString(value = test)]))" +
        "})"
    }
  }
}
