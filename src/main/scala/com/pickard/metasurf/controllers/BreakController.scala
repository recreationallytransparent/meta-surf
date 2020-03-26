package com.pickard.metasurf.controllers

import com.pickard.metasurf.Entities.Break
import com.pickard.metasurf.db.MongoDatabase
import org.mongodb.scala.model.Filters._

import scala.concurrent.{ExecutionContext, Future}

class BreakController(mongoDB: MongoDatabase) {
  def break(id: String)(implicit context: ExecutionContext): Future[Option[Break]] = {
    mongoDB.getCollection[Break]("surf-forecast-breaks").find(equal("id", id))
      .toFuture()
      .map(_.headOption)
  }
}
