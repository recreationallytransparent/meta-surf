package com.pickard.metasurf.controllers

import com.pickard.metasurf.Entities.Break
import com.pickard.metasurf.db.{CollectionNames, MongoDatabase, MongoSurfSelectors}
import com.typesafe.scalalogging.LazyLogging
import org.mongodb.scala.model.Filters._

import scala.concurrent.{ExecutionContext, Future}

class BreakController(mongoDB: MongoDatabase) extends CollectionController[Break](mongoDB, CollectionNames.breaks) with MongoSurfSelectors with LazyLogging {
  def break(id: String)(implicit context: ExecutionContext): Future[Option[Break]] = {
    mongoDB.getCollection[Break]("surf-forecast-breaks").find(equal("id", id))
      .toFuture()
      .map(_.headOption)
  }

  def byCountryRegion(countryName: String, regionName: String): Future[Iterable[Break]] = {
    logger.info(s"find breaks by country/region: $countryName/$regionName")
    mongoDB.getCollection[Break](CollectionNames.surfForecastBreaks)
      .find(breaksByCountryRegion(countryName, regionName))
      .toFuture()
  }

  def scrape(): Unit = {

  }
}
