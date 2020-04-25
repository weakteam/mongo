package io.github.weakteam.mongo.bson

import cats.Applicative
import cats.syntax.apply._
import io.github.weakteam.mongo.bson.BsonReaderResult._
import simulacrum.typeclass

@typeclass
trait BsonReader[+A] {
  def readBson(bson: BsonValue): BsonReaderResult[A]
}

object BsonReader {

  implicit val bsonReaderApplicativeInstance: Applicative[BsonReader] = new Applicative[BsonReader] {

    def pure[A](x: A): BsonReader[A] = _ => Success(BsonPath.__, x)

    def ap[A, B](ff: BsonReader[A => B])(fa: BsonReader[A]): BsonReader[B] = { bson =>
      ff.readBson(bson).ap(fa.readBson(bson))
    }
  }
}
