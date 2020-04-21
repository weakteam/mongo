package io.github.weakteam.mongo.bson

import cats.{Applicative, Eval, Traverse}
import cats.data.{NonEmptyList => Nel}
import cats.syntax.functor._
import cats.syntax.applicative._

sealed trait BsonReaderResult[+A] extends Product with Serializable

object BsonReaderResult {
  final case class Success[+A](value: A) extends BsonReaderResult[A]

  final case class Failure(errors: Nel[BsonError]) extends BsonReaderResult[Nothing]

  object Failure {
    def apply(err: BsonError, rest: BsonError*): Failure = Failure(Nel.of(err, rest: _*))
  }

  final case class PartialSuccess[+A](errors: Nel[BsonError], value: A) extends BsonReaderResult[A]

  implicit val bsonReaderResultApplicativeInstance: Applicative[BsonReaderResult] = new Applicative[BsonReaderResult] {
    def pure[A](x: A): BsonReaderResult[A] = Success(x)

    override def map[A, B](fa: BsonReaderResult[A])(f: A => B): BsonReaderResult[B] = fa match {
      case Success(value)                => Success(f(value))
      case Failure(errors)               => Failure(errors)
      case PartialSuccess(errors, value) => PartialSuccess(errors, f(value))
    }

    def prod[A, B, C](ff: BsonReaderResult[B])(fa: BsonReaderResult[A])(f: (B, A) => C): BsonReaderResult[C] =
      (ff, fa) match {
        case (Success(b), Success(a))                                 => Success(f(b, a))
        case (Success(b), PartialSuccess(errors, a))                  => PartialSuccess(errors, f(b, a))
        case (Success(_), Failure(errors))                            => Failure(errors)
        case (PartialSuccess(errors, b), Success(a))                  => PartialSuccess(errors, f(b, a))
        case (PartialSuccess(errors0, b), PartialSuccess(errors1, a)) => PartialSuccess(errors0 ::: errors1, f(b, a))
        case (PartialSuccess(errors0, _), Failure(errors1))           => Failure(errors0 ::: errors1)
        case (Failure(errors), Success(_))                            => Failure(errors)
        case (Failure(errors0), PartialSuccess(errors1, _))           => Failure(errors0 ::: errors1)
        case (Failure(errors0), Failure(errors1))                     => Failure(errors0 ::: errors1)
      }

    override def product[A, B](fa: BsonReaderResult[A], fb: BsonReaderResult[B]): BsonReaderResult[(A, B)] = {
      prod(fa)(fb)((a, b) => (a, b))
    }

    override def map2[A, B, Z](fa: BsonReaderResult[A], fb: BsonReaderResult[B])(
      f: (A, B) => Z
    ): BsonReaderResult[Z] = {
      prod(fa)(fb)(f)
    }

    def ap[A, B](ff: BsonReaderResult[A => B])(fa: BsonReaderResult[A]): BsonReaderResult[B] = {
      prod(ff)(fa)((f, a) => f(a))
    }
  }

  implicit val bsonReaderResultTraverseInstance: Traverse[BsonReaderResult] = new Traverse[BsonReaderResult] {
    def traverse[G[_]: Applicative, A, B](fa: BsonReaderResult[A])(f: A => G[B]): G[BsonReaderResult[B]] = {
      fa match {
        case Success(value)            => f(value).map(Success(_))
        case PartialSuccess(errors, _) => (Failure(errors): BsonReaderResult[B]).pure[G]
        case Failure(errors)           => (Failure(errors): BsonReaderResult[B]).pure[G]
      }
    }

    def foldLeft[A, B](fa: BsonReaderResult[A], b: B)(f: (B, A) => B): B = {
      fa match {
        case Success(value)           => f(b, value)
        case PartialSuccess(_, value) => f(b, value)
        case _                        => b
      }
    }

    def foldRight[A, B](fa: BsonReaderResult[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = {
      fa match {
        case Success(value)           => Eval.defer(f(value, lb))
        case PartialSuccess(_, value) => Eval.defer(f(value, lb))
        case _                        => lb
      }
    }
  }
}
