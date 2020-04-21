package io.github.weakteam.mongo.bson

import cats.Applicative
import cats.syntax.apply._
import com.github.ghik.silencer.silent
import io.github.weakteam.mongo.bson.BsonError.TypeMismatch
import io.github.weakteam.mongo.bson.BsonPath.EmptyBsonPath
import io.github.weakteam.mongo.bson.BsonReaderResult._
import simulacrum.typeclass

@typeclass
trait BsonReader[+A] {
  def readBson(bson: BsonValue): BsonReaderResult[A]
}

object BsonReader {

  @silent
  def lift[A: Class](pf: PartialFunction[BsonValue, A]): BsonReader[A] = { bson =>
    pf.andThen(Success(_)).applyOrElse(
      bson,
      value => Failure(TypeMismatch[A](implicitly, value, EmptyBsonPath(), None))
    )
  }

  implicit val bsonReaderApplicativeInstance: Applicative[BsonReader] = new Applicative[BsonReader] {

    def pure[A](x: A): BsonReader[A] = _ => Success(x)

    def ap[A, B](ff: BsonReader[A => B])(fa: BsonReader[A]): BsonReader[B] = { bson =>
      ff.readBson(bson).ap(fa.readBson(bson))
    }
  }
}
