package com.pickard.metasurf.db.codecs

import com.pickard.metasurf.Entities.BreakDetailsWebsiteUrl
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}

class BreakDetailsWebsiteUrlCodec extends Codec[BreakDetailsWebsiteUrl] {
  override def encode(writer: BsonWriter, value: BreakDetailsWebsiteUrl, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()

    writer.writeName("breakId")
    writer.writeString(value.breakId)

    writer.writeName("infoUrl")
    writer.writeString(value.infoUrl)

    writer.writeName("forecastsUrl")
    writer.writeString(value.forecastsUrl)

    writer.writeName("extendedForecastUrl")
    writer.writeString(value.extendedForecastUrl)

    writer.writeName("domain")
    writer.writeString(value.domain)

    writer.writeEndDocument()

  }

  override def getEncoderClass: Class[BreakDetailsWebsiteUrl] = classOf[BreakDetailsWebsiteUrl]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): BreakDetailsWebsiteUrl = {
    reader.readStartDocument()

    val id = reader.readObjectId("_id")
    val breakId = reader.readString("breakId")
    val infoUrl = reader.readString("infoUrl")
    val forecastsUrl = reader.readString("forecastsUrl")
    val extendedForecastUrl = reader.readString("extendedForecastUrl")

    val domain = reader.readString("domain")

    reader.readEndDocument()

    BreakDetailsWebsiteUrl(breakId, domain = domain,
      infoUrl = infoUrl,
      forecastsUrl = forecastsUrl,
      extendedForecastUrl = extendedForecastUrl
    )
  }
}
