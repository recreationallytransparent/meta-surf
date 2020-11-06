package com.pickard.metasurf.oneoffs

import com.pickard.metasurf.Entities.{Country, CountryRegions, Region}
import com.pickard.metasurf.db.{CollectionNames, MongoDatabase}
import com.pickard.metasurf.scrapers.JSoupDocumentProvider
import com.pickard.metasurf.scrapers.surfforecast.SurfForecastScraper
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object SeedCountryRegions extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val scraper = new SurfForecastScraper(new JSoupDocumentProvider)
    val mongoClient = new MongoDatabase

//    val result = mongoClient.insert(CollectionNames.countryRegions, CountryRegions(Country("wow", "wow"), Region("wow", "wow") :: Region("wow", "wow") :: Nil))
//    Await.result(result.toFuture(), 1.second)
    val countryRegions = scraper.getCountryRegions


    countryRegions match {
      case Left(e) => throw e
      case Right(list) =>
        logger.info(s"Found ${list.length} country regions")
        val inserts = mongoClient.insert[CountryRegions](CollectionNames.countryRegions, list)

        val result = Await.result(inserts.toFuture(), 1.minute)
        logger.info(s"finished, result = $result")
    }
  }
}
