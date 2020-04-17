package io.github.weakteam.bson

import cats.Show

sealed trait BsonSubtype extends Product with Serializable

object BsonSubtype {
  final case object GenericSubtype extends BsonSubtype
  final case object FunctionSubtype extends BsonSubtype
  final case object BinarySubtype extends BsonSubtype
  final case object UUIDSubtypeOld extends BsonSubtype
  final case object UUIDSubtype extends BsonSubtype
  final case object Md5Subtype extends BsonSubtype
  final case object BsonSubtype extends BsonSubtype
  final case class UserDefinedSubtype(value: Short) extends BsonSubtype

  implicit val bsonSubtypeShowInstance: Show[BsonSubtype] = {
    case GenericSubtype            => "BsonGeneric"
    case FunctionSubtype           => "BsonFunction"
    case BinarySubtype             => "BsonBinary"
    case UUIDSubtypeOld            => "BsonOldUUID"
    case UUIDSubtype               => "BsonUUID"
    case Md5Subtype                => "BsonMd5"
    case BsonSubtype               => "BsonSubtype"
    case UserDefinedSubtype(value) => s"BsonUserDefined(value = $value)"
  }
}
