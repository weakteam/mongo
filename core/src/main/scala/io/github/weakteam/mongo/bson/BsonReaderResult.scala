package io.github.weakteam.mongo.bson

import cats.{Applicative, ApplicativeError, Eval, Traverse}
import cats.data.{Ior, NonEmptyList => Nel}
import cats.syntax.functor._
import cats.syntax.applicative._
import io.github.weakteam.mongo.bson.BsonReaderResult._

sealed trait BsonReaderResult[+A] extends Product with Serializable { self =>
  def toIor: Ior[Nel[BsonErrorEntity], A] = self match {
    case BsonReaderResult.Success(value, _)                => Ior.right(value)
    case BsonReaderResult.Failure(errors)                  => Ior.left(errors)
    case BsonReaderResult.PartialSuccess(errors, _, value) => Ior.both(errors, value)
  }

  def withPath(pre: BsonPath): BsonReaderResult[A] = self match {
    case Success(value, _)                => Success(value, pre)
    case Failure(errors)                  => Failure(errors.map(_.prepath(pre)))
    case PartialSuccess(errors, _, value) => PartialSuccess(errors.map(_.prepath(pre)), pre, value)
  }

  def toOption: Option[A] = self match {
    case Success(value, _)           => Some(value)
    case PartialSuccess(_, _, value) => Some(value)
    case _                           => None
  }
}

object BsonReaderResult {
  final case class Success[+A](value: A, path: BsonPath = BsonPath.__) extends BsonReaderResult[A]

  final case class Failure(errors: Nel[BsonErrorEntity]) extends BsonReaderResult[Nothing]

  object Failure {
    def apply(err: (BsonError, BsonPath, BsonPath), rest: (BsonError, BsonPath, BsonPath)*): Failure =
      Failure(Nel.of((BsonErrorEntity.apply _).tupled(err), rest.map((BsonErrorEntity.apply _).tupled): _*))
    def apply(err: BsonError, rest: BsonError*): Failure =
      Failure(BsonErrorEntity(err), rest.map(BsonErrorEntity(_)): _*)
    def apply(err: BsonErrorEntity, rest: BsonErrorEntity*): Failure = new Failure(Nel.of(err, rest: _*))
  }

  final case class PartialSuccess[+A](errors: Nel[BsonErrorEntity], path: BsonPath, value: A)
    extends BsonReaderResult[A]

  implicit val bsonReaderResultApplicativeErrorInstance: ApplicativeError[BsonReaderResult, Nel[BsonErrorEntity]] =
    new ApplicativeError[BsonReaderResult, Nel[BsonErrorEntity]] {
      def pure[A](x: A): BsonReaderResult[A] = Success(x)

      override def map[A, B](fa: BsonReaderResult[A])(f: A => B): BsonReaderResult[B] = fa match {
        case Success(value, path)                => Success(f(value), path)
        case Failure(errors)                     => Failure(errors)
        case PartialSuccess(errors, path, value) => PartialSuccess(errors, path, f(value))
      }

      def prod[A, B, C](ff: BsonReaderResult[B])(fa: BsonReaderResult[A])(f: (B, A) => C): BsonReaderResult[C] =
        (ff, fa) match {
          case (Success(b, _), Success(a, path)) => Success(f(b, a), path)
          case (Success(b, _), PartialSuccess(errors, path, a)) =>
            PartialSuccess(errors, path, f(b, a))
          case (Success(_, _), Failure(errors)) => Failure(errors)
          case (PartialSuccess(errors, _, b), Success(a, path)) =>
            PartialSuccess(errors, path, f(b, a))
          case (PartialSuccess(errors0, _, b), PartialSuccess(errors1, path, a)) =>
            PartialSuccess(errors0 ::: errors1, path, f(b, a))
          case (PartialSuccess(errors0, _, _), Failure(errors1)) => Failure(errors0 ::: errors1)
          case (Failure(errors), Success(_, _))                  => Failure(errors)
          case (Failure(errors0), PartialSuccess(errors1, _, _)) => Failure(errors0 ::: errors1)
          case (Failure(errors0), Failure(errors1))              => Failure(errors0 ::: errors1)
        }

      override def product[A, B](fa: BsonReaderResult[A], fb: BsonReaderResult[B]): BsonReaderResult[(A, B)] = {
        prod(fa)(fb)((_, _))
      }

      def ap[A, B](ff: BsonReaderResult[A => B])(fa: BsonReaderResult[A]): BsonReaderResult[B] = {
        prod(ff)(fa)(_(_))
      }

      def raiseError[A](e: Nel[BsonErrorEntity]): BsonReaderResult[A] = Failure(e)

      def handleErrorWith[A](
        fa: BsonReaderResult[A]
      )(f: Nel[BsonErrorEntity] => BsonReaderResult[A]): BsonReaderResult[A] = {
        fa match {
          case Failure(errors)              => f(errors)
          case PartialSuccess(errors, _, _) => f(errors)
          case succ                         => succ
        }
      }
    }

  implicit val bsonReaderResultTraverseInstance: Traverse[BsonReaderResult] = new Traverse[BsonReaderResult] {
    def traverse[G[_]: Applicative, A, B](fa: BsonReaderResult[A])(f: A => G[B]): G[BsonReaderResult[B]] = {
      fa match {
        case Success(value, path)         => f(value).map((value: B) => Success(value, path))
        case PartialSuccess(errors, _, _) => (Failure(errors): BsonReaderResult[B]).pure[G]
        case Failure(errors)              => (Failure(errors): BsonReaderResult[B]).pure[G]
      }
    }

    def foldLeft[A, B](fa: BsonReaderResult[A], b: B)(f: (B, A) => B): B = {
      fa match {
        case Success(value, _)           => f(b, value)
        case PartialSuccess(_, _, value) => f(b, value)
        case _                           => b
      }
    }

    def foldRight[A, B](fa: BsonReaderResult[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = {
      fa match {
        case Success(value, _)           => Eval.defer(f(value, lb))
        case PartialSuccess(_, _, value) => Eval.defer(f(value, lb))
        case _                           => lb
      }
    }
  }
}
