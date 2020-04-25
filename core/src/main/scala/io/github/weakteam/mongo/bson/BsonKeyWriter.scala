package io.github.weakteam.mongo.bson

import simulacrum.typeclass
import io.github.weakteam.mongo.bson.BsonWriter.nonInheritedOps.toBsonWriterOps

@typeclass
trait BsonKeyWriter[-A] {
  def writeKey(key: A): String
}

object BsonKeyWriter {
  private[bson] final class KeyWriterOps(private val key: String) extends AnyVal {
    def :=[B: BsonWriter](value: B): (String, BsonValue) = {
      (key, value.writeBson)
    }
  }

  trait KeyWriterSyntax {
    implicit def toAdditionalKeyWriterOps[A](key: A)(implicit wr: BsonKeyWriter[A]): KeyWriterOps =
      new KeyWriterOps(wr.writeKey(key))
  }

  object KeyWriterSyntax extends KeyWriterSyntax

}
