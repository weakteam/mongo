package io.github.weakteam.mongo.bson

import cats.Show
import cats.syntax.show._
import cats.instances.string._

sealed trait BsonValue extends Product with Serializable

object BsonValue {
  final case object BsonNull extends BsonValue
  final case object BsonUndefined extends BsonValue
  final case object BsonMinKey extends BsonValue
  final case object BsonMaxKey extends BsonValue
  final case class BsonString(value: String) extends BsonValue
  final case class BsonRegex(value: String) extends BsonValue
  final case class BsonJS(value: String) extends BsonValue
  final case class BsonSymbol(value: String) extends BsonValue
  final case class BsonObjectId(value: String) extends BsonValue
  final case class BsonInt(value: Int) extends BsonValue
  final case class BsonLong(value: Long) extends BsonValue
  final case class BsonFloat(value: Double) extends BsonValue
  final case class BsonBoolean(value: Boolean) extends BsonValue
  final case class BsonTimestamp(counter: Int, value: Int) extends BsonValue
  final case class BsonPointer(value: Array[Byte]) extends BsonValue
  final case class BsonDecimal(value: BigDecimal) extends BsonValue
  final case class BsonDate(value: Long) extends BsonValue
  final case class BsonBinary(subtype: BsonSubtype, value: Array[Byte]) extends BsonValue
  final case class BsonArray(value: List[BsonValue]) extends BsonValue
  final case class BsonDocument(value: List[(String, BsonValue)]) extends BsonValue

  implicit lazy val bsonValueShowInstance: Show[BsonValue] = Show.show[BsonValue] {
    case BsonNull                      => "BsonNull"
    case BsonUndefined                 => "BsonUndefined"
    case BsonMinKey                    => "BsonMinKey"
    case BsonMaxKey                    => "BsonMaxKey"
    case BsonString(value)             => s"BsonString(value = $value)"
    case BsonRegex(value)              => s"BsonRegex(value = $value)"
    case BsonJS(value)                 => s"BsonJS(value = $value)"
    case BsonSymbol(value)             => s"BsonSymbol(value = $value)"
    case BsonObjectId(value)           => s"BsonObjectId(value = $value)"
    case BsonInt(value)                => s"BsonInt(value = $value)"
    case BsonLong(value)               => s"BsonLong(value = $value)"
    case BsonFloat(value)              => s"BsonFloat(value = $value)"
    case BsonBoolean(value)            => s"BsonBoolean(value = $value)"
    case BsonTimestamp(counter, value) => s"BsonTimestamp(counter = $counter, value = $value)"
    case BsonPointer(value)            => s"BsonPointer(value = [${value.mkString(", ")}])"
    case BsonDecimal(value)            => s"BsonDecimal(value = $value)"
    case BsonDate(value)               => s"BsonDate(value = $value)"
    case BsonBinary(subtype, value)    => show"BsonBinary(type = $subtype, value = [${value.mkString(", ")}])"
    case BsonArray(value) =>
      val s = value.map(bsonValueShowInstance.show).mkString(", ")
      s"BsonArray(values = [$s])"
    case BsonDocument(value) =>
      val s = value
        .map {
          case (key, value) =>
            s"(key = $key, value = ${bsonValueShowInstance.show(value)})"
        }
        .mkString(", ")
      s"BsonDocument(values = {$s})"
  }
}
