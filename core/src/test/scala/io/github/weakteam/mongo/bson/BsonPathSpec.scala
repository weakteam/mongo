package io.github.weakteam.mongo.bson

import cats.kernel.Monoid
import io.github.weakteam.mongo.bson.BsonError.{
  MinCountError,
  MultipleMatches,
  PathMismatch,
  TypeMismatch,
  ValidationError
}
import io.github.weakteam.mongo.bson.BsonPath.__
import io.github.weakteam.mongo.bson.BsonPathSpec._
import io.github.weakteam.mongo.bson.BsonPathNode.{KeyBsonPathNode, RootBsonPathNode}
import io.github.weakteam.mongo.bson.BsonValue.{BsonArray, BsonDocument, BsonInt, BsonNull}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

class BsonPathSpec extends AnyWordSpec with Matchers {
  "BsonPath#monoid" should {
    "return valid zero" in {
      Monoid[BsonPath].empty mustBe BsonPath(RootBsonPathNode)
    }

    "successful combine" in {
      val first  = (__ \ "key" \\ "help").::(RootBsonPathNode) \@ 1
      val second = ((__ \ "foo").::(RootBsonPathNode) \\ "test").::(RootBsonPathNode)
      Monoid[BsonPath].combine(first, second) mustBe BsonPath(KeyBsonPathNode("key")) \\ "help" \@ 1 \ "foo" \\ "test"
    }
  }

  "BsonPath#optimize" should {
    "successful optimize" in {
      val first  = (__ \ "key" \\ "help").::(RootBsonPathNode) \@ 1
      val second = ((__ \ "foo").::(RootBsonPathNode) \\ "test").::(RootBsonPathNode)
      second ::: first mustBe BsonPath(KeyBsonPathNode("key")) \\ "help" \@ 1 \ "foo" \\ "test"
      (second ::: first).optimize mustBe BsonPath(KeyBsonPathNode("key")) \\ "help" \@ 1 \ "foo" \\ "test"
    }
  }

  "BsonPath#readAt" should {

    "successful find root" in {
      val path = __
      path.readAt(bson) mustBe Right((path, bson))
    }

    "successful find by key in root" in {
      val path = BsonPath(KeyBsonPathNode("recurse"))
      path.readAt(bson) mustBe Right((path, innerDoc))
    }

    "unsuccessful find by key in root" in {
      val node = KeyBsonPathNode("fail")
      val path = node :: __
      path.readAt(bson) mustBe Left(BsonErrorEntity(PathMismatch, BsonPath(node), path))
    }

    "unsuccessful find by key in root (type mismatch)" in {
      val node = KeyBsonPathNode("fail")
      val path = node :: __
      path.readAt(array) mustBe Left(BsonErrorEntity(TypeMismatch("BsonDocument", array), BsonPath(node), path))
    }

    "successful find by key in inner" in {
      val path = __ \ "recurse" \ "array"
      path.readAt(bson) mustBe Right((path.optimize, array))
    }

    "unsuccessful find by key in inner" in {
      val path = __ \ "fail" \ "array"
      path.readAt(bson) mustBe Left(BsonErrorEntity(PathMismatch, path.optimize, (__ \ "fail").optimize))
    }

    "successful find elem in array" in {
      val path = __ \ "recurse" \ "array" \@ 3
      path.readAt(bson) mustBe Right((path.optimize, BsonInt(5)))
    }

    "unsuccessful find elem in array" in {
      val path = __ \ "recurse" \ "array" \@ 6
      path.readAt(bson) mustBe Left(BsonErrorEntity(MinCountError(6, 3), path.optimize, path.optimize))
    }

    "unsuccessful find elem in array (type mismatch)" in {
      val path = __ \ "recurse" \ "test1" \@ 6
      path.readAt(bson) mustBe Left(BsonErrorEntity(TypeMismatch("BsonArray", BsonNull), path.optimize, path.optimize))
    }

    "successful find elem by regex" in {
      val path = __ \ "recurse" \? ".*t2".r
      path.readAt(bson) mustBe Right(((__ \ "recurse" \ "test2").optimize, BsonNull))
    }

    "unsuccessful find elem by regex (no matches)" in {
      val path = __ \ "recurse" \? "^.*t$".r
      path.readAt(bson) mustBe Left(
        BsonErrorEntity(ValidationError("No matches for ^.*t$"), path.optimize, path.optimize)
      )
    }

    "unsuccessful find elem by regex (multi matches)" in {
      val path = __ \ "recurse" \? "^test.*$".r
      path.readAt(bson) mustBe Left(
        BsonErrorEntity(MultipleMatches(2), path.optimize, path.optimize)
      )
    }

    "unsuccessful find elem by regex (type mismatch)" in {
      val path = __ \ "recurse" \ "array" \? "test".r
      path.readAt(bson) mustBe Left(
        BsonErrorEntity(TypeMismatch("BsonDocument", array), path.optimize, path.optimize)
      )
    }

    "successful recursive find elem by key" in {
      val path = __ \\ "test1"
      path.readAt(bson) mustBe Right(((__ \ "recurse" \ "test1").optimize, BsonNull))
    }

    "unsuccessful recursive find elem by key" in {
      val path = __ \\ "test"
      path.readAt(bson) mustBe Left(BsonErrorEntity(PathMismatch, path.optimize, path.optimize))
    }

    "successful recursive find elem by regex" in {
      val path = __ \\? "^.*t1$".r
      path.readAt(bson) mustBe Right(((__ \ "recurse" \ "test1").optimize, BsonNull))
    }

    "unsuccessful recursive find elem by regex" in {
      val path = __ \\? ".*test".r
      path.readAt(bson) mustBe Left(BsonErrorEntity(PathMismatch, path.optimize, path.optimize))
    }

    "unsuccessful recursive find elem by regex (multi matches)" in {
      val path = __ \\? "test.+".r
      path.readAt(bson) mustBe Left(BsonErrorEntity(PathMismatch, path.optimize, path.optimize))
    }

    "unsuccessful recursive find elem by regex (all matches)" in {
      val path = __ \ "recurse" \\? ".+".r
      path.readAt(bson) mustBe Left(BsonErrorEntity(PathMismatch, path.optimize, path.optimize))
    }
  }
}

object BsonPathSpec {
  val array = BsonArray(BsonInt(3), BsonInt(4), BsonInt(5))
  val innerDoc = BsonDocument(
    "array" -> array,
    "test1" -> BsonNull,
    "test2" -> BsonNull
  )
  val bson = BsonDocument("recurse" -> innerDoc)
}
