package com.pickard.metasurf.db

import com.pickard.metasurf.Entities.{Break, Direction}
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import codecs._

object MongoCodecs {
  def codecs: List[Codec[_]] = new BreakCodec :: new BreakDetailsWebsiteUrlCodec :: Nil
}
