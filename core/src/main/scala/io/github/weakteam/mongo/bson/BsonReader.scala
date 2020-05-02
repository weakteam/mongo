package io.github.weakteam.mongo.bson

import java.util.regex.Pattern

import cats.data.{NonEmptyList => Nel}
import cats.ApplicativeError
import cats.syntax.apply._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import io.github.weakteam.mongo.bson.BsonError.TypeMismatch
import io.github.weakteam.mongo.bson.BsonReaderResult.{
  bsonReaderResultApplicativeErrorInstance,
  Failure,
  PartialSuccess,
  Success
}
import io.github.weakteam.mongo.bson.BsonValue._
import simulacrum.typeclass

import scala.util.matching.Regex

@typeclass
trait BsonReader[+A] {
  def readBson(bson: BsonValue): BsonReaderResult[A]
}

trait ReaderInstances {
  private[bson] def fromPfWithType[A](tpe: String, pf: PartialFunction[BsonValue, A]): BsonReader[A] = { bson =>
    pf.andThen(Success(_)).applyOrElse(bson, (other: BsonValue) => Failure(TypeMismatch(tpe, other)))
  }
  implicit val identityReader: BsonReader[BsonValue] = _.pure[BsonReaderResult]
  implicit val intReader: BsonReader[Int]            = fromPfWithType("BsonInt", { case BsonInt(int) => int })
  implicit val stringReader: BsonReader[String]      = fromPfWithType("BsonString", { case BsonString(string) => string })
  implicit val longReader: BsonReader[Long]          = fromPfWithType("BsonLong", { case BsonLong(long) => long })
  implicit val doubleReader: BsonReader[Double]      = fromPfWithType("BsonFloat", { case BsonFloat(double) => double })
  implicit val floatReader: BsonReader[Float] =
    fromPfWithType("BsonFloat", { case BsonFloat(double) => double.toFloat })
  implicit val booleanReader: BsonReader[Boolean] = fromPfWithType("BsonBoolean", { case BsonBoolean(bool) => bool })
  implicit val regexReader: BsonReader[Regex]     = fromPfWithType("BsonRegex", { case BsonRegex(string)   => string.r })
  implicit val patternReader: BsonReader[Pattern] = fromPfWithType("BsonRegex", {
    case BsonRegex(string) => string.r.pattern
  })
}

trait LowPriorityReaderInstances {
  implicit def optionReader[A](implicit R: BsonReader[A]): BsonReader[Option[A]] = {
    case BsonNull => Success(None)
    case bson     => R.readBson(bson).map(Some(_))
  }

  implicit def eitherReader[L, R](implicit LR: BsonReader[L], RR: BsonReader[R]): BsonReader[Either[L, R]] = { bson =>
    (RR.readBson(bson), LR.readBson(bson)) match {
      case (Success(right, _), _)                                         => Success(Right(right))
      case (_, Success(left, _))                                          => Success(Left(left))
      case (Failure(errors0), Failure(errors1))                           => Failure(errors0 ::: errors1)
      case (PartialSuccess(errors0, _, _), PartialSuccess(errors1, _, _)) => Failure(errors0 ::: errors1)
      case (PartialSuccess(errors0, path, value), _)                      => PartialSuccess(errors0, path, Right(value))
      case (_, PartialSuccess(errors0, path, value))                      => PartialSuccess(errors0, path, Left(value))
    }
  }

  implicit def listReader[A](implicit R: BsonReader[A]): BsonReader[List[A]] = {
    case BsonArray(list) =>
      val (err, acc) = list.foldLeft((List.newBuilder[BsonErrorEntity], List.newBuilder[A])) {
        case ((err, acc), item) =>
          R.readBson(item) match {
            case Success(value, _)                => (err, acc += value)
            case Failure(errors)                  => (err ++= errors.toList, acc)
            case PartialSuccess(errors, _, value) => (err ++= errors.toList, acc += value)
          }
      }
      (err.result(), acc.result()) match {
        case (Nil, Nil)                   => Success(Nil)
        case (head :: rest, Nil)          => Failure(head, rest: _*)
        case (head :: rest, acc @ _ :: _) => PartialSuccess(errors = Nel.of(head, rest: _*), value = acc)
        case (Nil, acc @ _ :: _)          => Success(acc)
      }
    case other => Failure(TypeMismatch("BsonList", other))
  }

  implicit def mapReader[A](implicit R: BsonReader[A]): BsonReader[Map[String, A]] = {
    case BsonDocument(map) =>
      val (err, acc) = map.foldLeft((List.newBuilder[BsonErrorEntity], List.newBuilder[(String, A)])) {
        case ((err, acc), (k, item)) =>
          R.readBson(item) match {
            case Success(value, _)                => (err, acc += (k -> value))
            case Failure(errors)                  => (err ++= errors.toList, acc)
            case PartialSuccess(errors, _, value) => (err ++= errors.toList, acc += (k -> value))
          }
      }
      (err.result(), acc.result()) match {
        case (Nil, Nil)                   => Success(Map.empty)
        case (head :: rest, Nil)          => Failure(head, rest: _*)
        case (head :: rest, acc @ _ :: _) => PartialSuccess(errors = Nel.of(head, rest: _*), value = acc.toMap)
        case (Nil, acc @ _ :: _)          => Success(acc.toMap)
      }
    case other => Failure(TypeMismatch("BsonList", other))
  }

  implicit def setReader[A](implicit R: BsonReader[A]): BsonReader[Set[A]] = {
    case BsonArray(list) =>
      val (err, acc) = list.foldLeft((List.newBuilder[BsonErrorEntity], List.newBuilder[A])) {
        case ((err, acc), item) =>
          R.readBson(item) match {
            case Success(value, _)                => (err, acc += value)
            case Failure(errors)                  => (err ++= errors.toList, acc)
            case PartialSuccess(errors, _, value) => (err ++= errors.toList, acc += value)
          }
      }
      (err.result(), acc.result()) match {
        case (Nil, Nil)                   => Success(Set.empty)
        case (head :: rest, Nil)          => Failure(head, rest: _*)
        case (head :: rest, acc @ _ :: _) => PartialSuccess(errors = Nel.of(head, rest: _*), value = acc.toSet)
        case (Nil, acc @ _ :: _)          => Success(acc.toSet)
      }
    case other => Failure(TypeMismatch("BsonList", other))
  }
}

object BsonReader extends ReaderInstances with LowPriorityReaderInstances {

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
