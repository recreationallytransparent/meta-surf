package com.pickard.metasurf.db.codecs

import com.pickard.metasurf.Entities.{Country, CountryRegions, Region}
import org.bson.{BsonReader, BsonType, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}

class CountryRegionsCodec extends Codec[CountryRegions] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): CountryRegions = {
    reader.readStartDocument()
    reader.readObjectId("_id")
    reader.readName("country")
    reader.readStartDocument()
    val countryId = reader.readString("id")
    val countryName = reader.readString("name")
    reader.readEndDocument()
    reader.readName()
    reader.readStartArray()
    val regions = scala.collection.mutable.MutableList.empty[Region]
    while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      reader.readStartDocument()
      val regionId = reader.readString("id")
      val regionName = reader.readString("name")
      reader.readEndDocument()
      regions += Region(regionId, regionName)
    }
    reader.readEndArray()
    reader.readEndDocument()
    CountryRegions(Country(countryId, countryName), regions.toList)
  }

  override def encode(writer: BsonWriter, value: CountryRegions, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()
    writer.writeStartDocument("country")
    writer.writeString("id", value.country.id)
    writer.writeString("name", value.country.name)
    writer.writeEndDocument()
    writer.writeName("regions")
    writer.writeStartArray()
    value.regions.foreach(region => {
      writer.writeStartDocument()
      writer.writeString("id", region.id)
      writer.writeString("name", region.name)
      writer.writeEndDocument()
    })
    writer.writeEndArray()
    writer.writeEndDocument()
  }

  override def getEncoderClass: Class[CountryRegions] = classOf[CountryRegions]
}
