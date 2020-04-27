package io.github.weakteam.mongo.bson

import java.util.regex.Pattern

import cats.Contravariant
import io.github.weakteam.mongo.bson.BsonSubtype.UserDefinedSubtype
import io.github.weakteam.mongo.bson.BsonValue.{
  BsonArray,
  BsonBinary,
  BsonBoolean,
  BsonDocument,
  BsonFloat,
  BsonInt,
  BsonLong,
  BsonNull,
  BsonRegex,
  BsonString
}
import simulacrum.typeclass

import scala.util.matching.Regex

@typeclass
trait BsonWriter[-T] { self =>
  def writeBson(arg: T): BsonValue
}

trait Instances {
  implicit val identityWriter: BsonWriter[BsonValue] = identity(_)
  implicit val intWriter: BsonWriter[Int]            = BsonInt(_)
  implicit val stringWriter: BsonWriter[String]      = BsonString(_)
  implicit val longWriter: BsonWriter[Long]          = BsonLong(_)
  implicit val doubleWriter: BsonWriter[Double]      = BsonFloat(_)
  implicit val floatWriter: BsonWriter[Float]        = float => BsonFloat(float.toDouble)
  implicit val booleanWriter: BsonWriter[Boolean]    = BsonBoolean(_)
  implicit val regexWriter: BsonWriter[Regex]        = reg => BsonRegex(reg.regex)
  implicit val patternWriter: BsonWriter[Pattern]    = reg => BsonRegex(reg.pattern())
}

trait LowPriorityInstances {

  implicit def optionWriter[A](implicit W: BsonWriter[A]): BsonWriter[Option[A]] = {
    case Some(value) => W.writeBson(value)
    case _           => BsonNull
  }

  implicit def eitherWriter[L, R](implicit LW: BsonWriter[L], RW: BsonWriter[R]): BsonWriter[Either[L, R]] = {
    _.fold(LW.writeBson, RW.writeBson)
  }

  implicit def listWriter[A](implicit W: BsonWriter[A]): BsonWriter[List[A]] = { list =>
    BsonArray(list.map(W.writeBson))
  }

  implicit def setWriter[A](implicit W: BsonWriter[A]): BsonWriter[Set[A]] = { set =>
    BsonArray(set.map(W.writeBson).toList)
  }

  implicit def mapWriter[K, V](implicit K: BsonKeyWriter[K], W: BsonWriter[V]): BsonWriter[Map[K, V]] = { map =>
    BsonDocument(map.map { case (k, v) => (K.writeKey(k), W.writeBson(v)) })
  }

  implicit def enum[T <: Enumeration](e: T): BsonWriter[e.Value] = { e =>
    BsonBinary(UserDefinedSubtype(0x05), e.id.toString.getBytes("UTF-8"))
  }
}

object BsonWriter extends Instances with LowPriorityInstances {
  def instance[A](f: A => BsonValue): BsonWriter[A] = f(_)

  implicit val bsonWriterContravariantInstance: Contravariant[BsonWriter] = new Contravariant[BsonWriter] {
    def contramap[A, B](fa: BsonWriter[A])(f: B => A): BsonWriter[B] = { arg =>
      fa.writeBson(f(arg))
    }
  }
}
