package io.github.weakteam.mongo.bson

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cats.syntax.show._
import BsonSubtype._

class BsonSubtypeSpec extends AnyWordSpec with Matchers {
  "BsonSubtypeShow" should {
    "return appropriate value for Generic" in {
      show"$GenericSubtype" mustBe "BsonGeneric"
    }

    "return appropriate value for Function" in {
      show"$FunctionSubtype" mustBe "BsonFunction"
    }

    "return appropriate value for Binary" in {
      show"$BinarySubtype" mustBe "BsonBinary"
    }

    "return appropriate value for Old UUID" in {
      show"$UUIDSubtypeOld" mustBe "BsonOldUUID"
    }

    "return appropriate value for UUID" in {
      show"$UUIDSubtype" mustBe "BsonUUID"
    }

    "return appropriate value for Md5" in {
      show"$Md5Subtype" mustBe "BsonMd5"
    }

    "return appropriate value for BsonSubtype" in {
      show"$BsonCryptSubtype" mustBe "BsonCryptSubtype"
    }

    "return appropriate value for UserDefinedSubtype" in {
      show"${UserDefinedSubtype(0xf0)}" mustBe "BsonUserDefined(value = 240)"
    }
  }
}
