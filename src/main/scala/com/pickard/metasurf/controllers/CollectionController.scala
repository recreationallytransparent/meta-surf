package com.pickard.metasurf.controllers

import com.pickard.metasurf.db.MongoDatabase
import org.mongodb.scala.MongoCollection

import scala.concurrent.Future
import scala.reflect.ClassTag

class CollectionController[T](mongo: MongoDatabase, collectionName: String) extends Controller {
  protected def getCollection()(implicit tag: ClassTag[T]): MongoCollection[T] =  mongo.getCollection[T](collectionName)
  def get()(implicit tag: ClassTag[T]): Future[Seq[T]] = getCollection().find().toFuture()
}
