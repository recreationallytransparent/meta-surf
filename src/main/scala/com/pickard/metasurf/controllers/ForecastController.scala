package com.pickard.metasurf.controllers

import com.pickard.metasurf.Entities.{BreakDetailsWebsiteUrl, BreakForecast, MetaForecast}
import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.scrapers.SurfScraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.mongodb.scala.{FindObservable, MongoCollection}
import org.mongodb.scala.model.Filters._

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ForecastController(browser: JsoupBrowser, mongoDB: MongoDatabase, scrapers: List[SurfScraper]) {
  private val websiteUrls: MongoCollection[BreakDetailsWebsiteUrl] =
    mongoDB.getCollection[BreakDetailsWebsiteUrl]("break-details-website-urls")

  private val domainToScraper: Map[String, List[SurfScraper]] = scrapers.groupBy(_.domain)

  def forId(breakId: String): Future[Option[MetaForecast]] = {
    val urls: FindObservable[BreakDetailsWebsiteUrl] = websiteUrls.find(equal("breakId", breakId))

    urls
      .toFuture()
      .flatMap(urls => {
        val futureForecasts = urls.map(url => Future {
          lazy val doc = browser.get(url.url)
          val maybeForecasts: Option[List[(String, Either[Throwable, BreakForecast])]] =
            domainToScraper.find(_._1 == url.domain)
              .map({
                case (domain, scrapers: List[SurfScraper]) =>
                  scrapers.map(scraper => (domain, scraper.getForecast(breakId, doc)))
              })

          maybeForecasts.map(_.map {
            case (_, Left(e)) => throw e
            case (domain, Right(forecast)) => (domain, forecast)
          })
        })

        Future.sequence(futureForecasts)
      })
      .map((forecasts: Seq[Option[List[(String, BreakForecast)]]]) => {
        val domainsAndForecasts: Seq[(String, BreakForecast)] = forecasts.foldLeft(List.empty[(String, BreakForecast)])((acc, x) => x match {
          case Some(l) => acc ::: l
          case None => acc
        })

        if (domainsAndForecasts.nonEmpty) Some(MetaForecast(domainsAndForecasts.toList))
        else None
      })
  }
}
