package com.pickard.metasurf.controllers

import com.pickard.metasurf.Entities.{BreakDetails, BreakDetailsWebsiteUrl}
import com.pickard.metasurf.db.{CollectionNames, MongoDatabase}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

class BreakUrlController(mongoDb: MongoDatabase)(implicit context: ExecutionContext)
  extends CollectionController[BreakDetailsWebsiteUrl](mongoDb, CollectionNames.breakUrls) with LazyLogging {
  import org.mongodb.scala.model.Filters._

  def search(term: String): Future[Iterable[BreakDetailsWebsiteUrl]] = {
    logger.info(s"search = $term")
    getCollection().find(text(term)).toFuture()
  }

  def add(item: BreakDetailsWebsiteUrl): Future[BreakDetailsWebsiteUrl] = {
    logger.info(s"put $item")
    getCollection().find(and(equal("breakId", item.breakId), equal("domain", item.domain)))
        .headOption()
        .flatMap({
          case Some(existing) =>
            logger.info(s"url with id ${item.breakId} and domain ${item.domain} already exists, updating")
            getCollection()
            .replaceOne(and(equal("breakId", item.breakId), equal("domain", item.domain)), item)
            .map(x => if (x.getMatchedCount > 0) {
              item
            } else throw new RuntimeException("Could not preform update"))
            .head()
          case _ =>
            logger.info(s"creating new url with $item")
            getCollection().insertOne(item).toFuture().map(_ => item)
        })

  }
}
