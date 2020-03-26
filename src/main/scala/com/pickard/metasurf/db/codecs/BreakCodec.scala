package com.pickard.metasurf.db.codecs

import com.pickard.metasurf.Entities.{Break, Direction}
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}

class BreakCodec extends Codec[Break] {
  override def encode(writer: BsonWriter, value: Break, encoderContext: EncoderContext): Unit = {
  writer.writeStartDocument()

  writer.writeName("id")
  writer.writeString(value.id)

  writer.writeName("slug")
  writer.writeString(value.slug)

  writer.writeName("name")
  writer.writeString(value.name)

  writer.writeName("region")
  writer.writeString(value.region)

  writer.writeName("country")
  writer.writeString(value.country)

  writer.writeName("lngLat")
  writer.writeStartArray()
  writer.writeDouble(value.lngLat._1)
  writer.writeDouble(value.lngLat._2)
  writer.writeEndArray()

  writer.writeName("style")
  writer.writeString(value.style)

  writer.writeName("bestSwell")
  writer.writeString(value.bestSwell.toString)

  writer.writeName("bestWind")
  writer.writeString(value.bestWind.toString)

  writer.writeEndDocument()
}

  override def getEncoderClass: Class[Break] = classOf[Break]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): Break = {
  reader.readStartDocument()
  val _id = reader.readObjectId("_id")
  val id = reader.readString("id")
  val slug = reader.readString("slug")
  val name = reader.readString("name")
  val region = reader.readString("region")
  val country = reader.readString("country")
  reader.readName()
  reader.readStartArray()
  val lngLat = (reader.readDouble(), reader.readDouble)
  reader.readEndArray()
  val style = reader.readString("style")
  val bestSwell = Direction.withName(reader.readString("bestSwell"))
  val bestWind = Direction.withName(reader.readString("bestWind"))
  reader.readEndDocument()

  Break(id, slug, name, region, country, lngLat, style, bestSwell, bestWind)
}
}
