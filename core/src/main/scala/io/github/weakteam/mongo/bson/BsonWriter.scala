package io.github.weakteam.mongo.bson

import cats.Contravariant
import simulacrum.typeclass

@typeclass
trait BsonWriter[-T] { self =>
  def writeBson(arg: T): BsonValue
}

object BsonWriter {
  def instance[A](f: A => BsonValue): BsonWriter[A] = f(_)

  implicit val bsonWriterContravariantInstance: Contravariant[BsonWriter] = new Contravariant[BsonWriter] {
    def contramap[A, B](fa: BsonWriter[A])(f: B => A): BsonWriter[B] = { arg =>
      fa.writeBson(f(arg))
    }
  }
}
