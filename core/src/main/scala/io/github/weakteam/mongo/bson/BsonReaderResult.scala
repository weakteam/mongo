package io.github.weakteam.mongo.bson

import cats.{Applicative, Eval, Traverse}
import cats.data.{Ior, NonEmptyList => Nel}
import cats.syntax.functor._
import cats.syntax.applicative._

sealed trait BsonReaderResult[+A] extends Product with Serializable { self =>
  def toIor: Ior[Nel[BsonErrorEntity], A] = self match {
    case BsonReaderResult.Success(_, value)                => Ior.right(value)
    case BsonReaderResult.Failure(errors)                  => Ior.left(errors)
    case BsonReaderResult.PartialSuccess(errors, _, value) => Ior.both(errors, value)
  }
}

object BsonReaderResult {
  final case class Success[+A](path: BsonPath, value: A) extends BsonReaderResult[A]

  final case class Failure(errors: Nel[BsonErrorEntity]) extends BsonReaderResult[Nothing]

  object Failure {
    def apply(err: (BsonPath, BsonPath, BsonError), rest: (BsonPath, BsonPath, BsonError)*): Failure =
      Failure(Nel.of((BsonErrorEntity.apply _).tupled(err), rest.map((BsonErrorEntity.apply _).tupled): _*))
    def apply(err: BsonError, rest: BsonError*): Failure =
      Failure(BsonErrorEntity(error = err), rest.map(e => BsonErrorEntity(error = e)): _*)
    def apply(err: BsonErrorEntity, rest: BsonErrorEntity*): Failure = new Failure(Nel.of(err, rest: _*))
  }

  final case class PartialSuccess[+A](errors: Nel[BsonErrorEntity], path: BsonPath, value: A)
    extends BsonReaderResult[A]

  implicit val bsonReaderResultApplicativeInstance: Applicative[BsonReaderResult] = new Applicative[BsonReaderResult] {
    def pure[A](x: A): BsonReaderResult[A] = Success(BsonPath.__, x)

    override def map[A, B](fa: BsonReaderResult[A])(f: A => B): BsonReaderResult[B] = fa match {
      case Success(path, value)                => Success(path, f(value))
      case Failure(errors)                     => Failure(errors)
      case PartialSuccess(errors, path, value) => PartialSuccess(errors, path, f(value))
    }

    def prod[A, B, C](ff: BsonReaderResult[B])(fa: BsonReaderResult[A])(f: (B, A) => C): BsonReaderResult[C] =
      (ff, fa) match {
        case (Success(path0, b), Success(path, a)) => Success(path ::: path0, f(b, a))
        case (Success(path0, b), PartialSuccess(errors, path, a)) =>
          PartialSuccess(errors, path ::: path0, f(b, a))
        case (Success(_, _), Failure(errors)) => Failure(errors)
        case (PartialSuccess(errors, path0, b), Success(path, a)) =>
          PartialSuccess(errors, path ::: path0, f(b, a))
        case (PartialSuccess(errors0, path0, b), PartialSuccess(errors1, path, a)) =>
          PartialSuccess(errors0 ::: errors1, path ::: path0, f(b, a))
        case (PartialSuccess(errors0, _, _), Failure(errors1)) => Failure(errors0 ::: errors1)
        case (Failure(errors), Success(_, _))                  => Failure(errors)
        case (Failure(errors0), PartialSuccess(errors1, _, _)) => Failure(errors0 ::: errors1)
        case (Failure(errors0), Failure(errors1))              => Failure(errors0 ::: errors1)
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
        case Success(path, value)         => f(value).map(Success(path, _))
        case PartialSuccess(errors, _, _) => (Failure(errors): BsonReaderResult[B]).pure[G]
        case Failure(errors)              => (Failure(errors): BsonReaderResult[B]).pure[G]
      }
    }

    def foldLeft[A, B](fa: BsonReaderResult[A], b: B)(f: (B, A) => B): B = {
      fa match {
        case Success(_, value)           => f(b, value)
        case PartialSuccess(_, _, value) => f(b, value)
        case _                           => b
      }
    }

    def foldRight[A, B](fa: BsonReaderResult[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = {
      fa match {
        case Success(_, value)           => Eval.defer(f(value, lb))
        case PartialSuccess(_, _, value) => Eval.defer(f(value, lb))
        case _                           => lb
      }
    }
  }
}
