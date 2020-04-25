package io.github.weakteam.mongo.bson

import cats.{Eval, Monoid}
import cats.data.NonEmptyList
import cats.syntax.either._
import io.github.weakteam.mongo.bson.BsonPathNode._
import io.github.weakteam.mongo.bson.BsonReaderResult._

import scala.util.matching.Regex

final case class BsonPath private (paths: NonEmptyList[BsonPathNode]) { self =>
  def \@(id: Int): BsonPath             = BsonPath(IdBsonPathNode(id) :: paths)
  def \(key: String): BsonPath          = BsonPath(KeyBsonPathNode(key) :: paths)
  def \\(key: String): BsonPath         = BsonPath(RecursiveKeyBsonPathNode(key) :: paths)
  def \?(regex: Regex): BsonPath        = BsonPath(RegexBsonPathNode(regex) :: paths)
  def \\?(regex: Regex): BsonPath       = BsonPath(RecursiveRegexBsonPathNode(regex) :: paths)
  def ++(bsonPath: BsonPath): BsonPath  = BsonPath(bsonPath.paths ::: paths).optimize
  def :::(bsonPath: BsonPath): BsonPath = self ++ bsonPath

  def ::(path: BsonPathNode): BsonPath = BsonPath(path) ::: self

  private[bson] def optimize: BsonPath = {
    BsonPath(
      NonEmptyList
        .fromList(paths.filterNot(_ == RootBsonPathNode))
        .getOrElse(NonEmptyList.of(RootBsonPathNode))
    )
  }

  private[bson] def readAt(bson: BsonValue): Either[BsonErrorEntity, (BsonPath, BsonValue)] = {
    val opts = self.optimize
    opts.paths
      .foldRight(Eval.now((BsonPath.__, bson).asRight[(BsonPath, BsonError)])) {
        case (nextNode, ev) =>
          ev.map {
            _.flatMap {
              case (prev, value) =>
                nextNode
                  .readPath(value)
                  .leftMap((nextNode :: prev, _))
                  .map { case (path, v) => (prev ++ path, v) }
            }
          }
      }
      .value
      .leftMap { case (path, err) => BsonErrorEntity(err, opts, path) }
  }

  def read[A](implicit R: BsonReader[A]): BsonReader[A] = { bson =>
    readAt(bson) match {
      case Left(err)            => Failure(err)
      case Right((path0, bson)) => R.readBson(bson).prepath(path0)
    }

  }
}

object BsonPath {
  def __ : BsonPath                                              = BsonPath(RootBsonPathNode)
  def apply(path0: BsonPathNode, paths: BsonPathNode*): BsonPath = new BsonPath(NonEmptyList.of(path0, paths: _*))

  implicit val bsonPathMonoid: Monoid[BsonPath] = new Monoid[BsonPath] {
    def empty: BsonPath = __

    def combine(x: BsonPath, y: BsonPath): BsonPath = y ::: x
  }
}
