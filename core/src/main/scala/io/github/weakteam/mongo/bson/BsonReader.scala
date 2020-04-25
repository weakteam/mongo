package io.github.weakteam.mongo.bson

import cats.data.{NonEmptyList => Nel}
import cats.ApplicativeError
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import io.github.weakteam.mongo.bson.BsonReaderResult.{bsonReaderResultApplicativeErrorInstance, Failure, Success}
import simulacrum.typeclass

@typeclass
trait BsonReader[+A] {
  def readBson(bson: BsonValue): BsonReaderResult[A]
}

object BsonReader {

  implicit val bsonReaderApplicativeErrorInstance: ApplicativeError[BsonReader, Nel[BsonErrorEntity]] =
    new ApplicativeError[BsonReader, Nel[BsonErrorEntity]] {
      override def map[A, B](fa: BsonReader[A])(f: A => B): BsonReader[B] = fa.readBson(_).map(f)

      def pure[A](x: A): BsonReader[A] = _ => Success(value = x)

      def ap[A, B](ff: BsonReader[A => B])(fa: BsonReader[A]): BsonReader[B] = { bson =>
        ff.readBson(bson).ap(fa.readBson(bson))
      }

      def raiseError[A](e: Nel[BsonErrorEntity]): BsonReader[A] = _ => Failure(e)

      def handleErrorWith[A](fa: BsonReader[A])(f: Nel[BsonErrorEntity] => BsonReader[A]): BsonReader[A] =
        bson => fa.readBson(bson).handleErrorWith(f(_).readBson(bson))
    }
}
