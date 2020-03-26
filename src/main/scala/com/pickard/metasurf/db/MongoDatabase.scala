package com.pickard.metasurf.db

import com.mongodb.client.model.BulkWriteOptions
import com.pickard.metasurf.Entities.Break
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.{CodecRegistries, CodecRegistry}
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, SingleObservable}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY

import scala.reflect.ClassTag
import org.mongodb.scala._
import org.mongodb.scala.model.InsertManyOptions

import scala.collection.JavaConverters._

class MongoDatabase {
  private val codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(MongoCodecs.codecs.asJava), DEFAULT_CODEC_REGISTRY)
  private val client = MongoClient()
  private val database: org.mongodb.scala.MongoDatabase = client.getDatabase("meta-surf").withCodecRegistry(codecRegistry)

  def getCollection[T](collection: String)(implicit tag: ClassTag[T]): MongoCollection[T] =
    database.getCollection[T](collection)

  def insert[T](collection: String, item: T)(implicit tag: ClassTag[T]): SingleObservable[Completed] =
    database.getCollection[T](collection).insertOne(item)

  def insert[T](collection: String, items: Seq[T], ordered: Boolean = false)(implicit tag: ClassTag[T]): SingleObservable[Completed] = {
    //    database.getCollection[T](collection).insertMany(items)
    val options = new InsertManyOptions().ordered(ordered)
    database.getCollection[T](collection).insertMany(items, options)
  }
}
