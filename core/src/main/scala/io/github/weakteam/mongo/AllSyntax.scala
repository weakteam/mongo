package io.github.weakteam.mongo

import io.github.weakteam.mongo.bson.{BsonKeyWriter, BsonReader, BsonWriter}

trait AllSyntax
  extends BsonWriter.ToBsonWriterOps
  with BsonKeyWriter.ToBsonKeyWriterOps
  with BsonKeyWriter.KeyWriterSyntax
  with BsonReader.ToBsonReaderOps
