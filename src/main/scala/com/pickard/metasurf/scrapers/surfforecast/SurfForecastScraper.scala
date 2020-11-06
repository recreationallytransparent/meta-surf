package com.pickard.metasurf.scrapers.surfforecast

import com.pickard.metasurf.Entities._
import com.pickard.metasurf.scrapers.{DocumentProvider, SurfScraper}
import com.typesafe.scalalogging.LazyLogging
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model._

import scala.concurrent.Future
import scala.util._

object SurfForecastScraper {

}

class SurfForecastScraper(documentProvider: DocumentProvider) extends SurfScraper with SurfForecastScraperProvider with LazyLogging {
  override def get(url: String): Document = documentProvider.get(url)

  override def domain: String = "surf-forecast.com"

  override def baseUrl: String = "https://www.surf-forecast.com"

  override def getForecast(id: String, doc: Document): Either[Throwable, BreakForecast] = {
    logger.info(s"scrape forecast: $id, ${doc.location}")
    val dataMap: Map[String, Element] = (doc >> elementList("table tr"))
      .filter(_.hasAttr("data-row-name"))
      .map(r => r.attr("data-row-name") -> r)
      .toMap

    val maybeForecast: Option[BreakForecast] = for (
      times <- dataMap.get("time").map(parseTimes);
      rating <- dataMap.get("rating").map(parseRating);
      waves <- dataMap.get("wave-height").map(parseWaves);
      windState <- dataMap.get("wind-state");
      wind <- dataMap.get("wind").map(x => parseWind(x, windState));
      highTide <- dataMap.get("high-tide").map(x => parseTides(x, times));
      lowTide <- dataMap.get("low-tide").map(x => parseTides(x, times))
    ) yield {
      val forecast = (times.zipWithIndex zip rating zip waves zip wind).map(t => (t._1._1._1._1, t._1._1._1._2, t._1._1._2, t._1._2, t._2)).map({
        case (time, timeIndex, rating, waveDetails, windDetails) =>
          val hTide = highTide.find(_._2 == timeIndex).map(_._1)
          val lTide = lowTide.find(_._2 == timeIndex).map(_._1)
          BreakDetails(id, time, Math.round(100 * (rating / 10)).toInt, waveDetails, windDetails, hTide, lTide)
      })

      BreakForecast(id, forecast = forecast)
    }

    maybeForecast match {
      case Some(x) => Right(x)
      case None => Left(new Exception(s"Could not parse forecast for break with id ${id}"))
    }
  }

  override def getInfo(id: String, doc: Document): Either[Throwable, Break] = Try {
    val nameAndRegion = doc >> element("#contdiv .column .h1cont")
    val nameAndRegionPattern = "(.*) Surf Forecast and Surf Reports \\((.*)[,| –] (.*)\\)".r
    val (name, region, country) = nameAndRegion.text match {
      case nameAndRegionPattern(n, r, c) => (n, r, c)
      case _ => throw new RuntimeException(s"Could not parse name and region: ${nameAndRegion.text}")
    }

    val summary = doc >> element("div.spot_id_summary")
    val summaryPattern = "Type: (.*) Reliability: (.*)? Best: Swell (.*) \\| Wind (.*) Sea.*".r
    val (style, bestSwell, bestWind) = summary.text match {
      case summaryPattern(s, r, bs, bw) => (s, Direction.withName(bs), Direction.withName(bw))
      case _ => throw new RuntimeException(s"Could not parse summary: ${summary.text}")
    }

    Break(id, doc.location.split("/").last, name, region, country, (-180, -180), style, bestSwell, bestWind)
  } match {
    case Success(value) => Right(value)
    case Failure(e) => Left(e)
  }

  override def infoUrl(slug: String): String = s"https://surf-forecast.com/breaks/${slug}"

  override def forecastUrl(slug: String): String = s"https://surf-forecast.com/breaks/${slug}/forecasts/latest"
  override def extendedForecastsUrl(slug: String): String = s"https://surf-forecast.com/breaks/${slug}/forecasts/latest/six_day"

  override def getCountryRegions: Either[Throwable, List[CountryRegions]] = Try {
    val doc = get(baseUrl)
    val countries: List[Country] = (doc >> elementList("#country_id option")).map(e => Country(e.attr("value"), e.text))

    logger.info(s"There are ${countries.length} countries to query")

    countries.par.map(country => {
      val regions = getRegions(country)
      logger.info(s"found ${regions.length} regions for country ${country.name}")
      CountryRegions(country, regions)
    }).toList
  }.toEither

  def getRegions(country: Country): List[Region] = {
    val url = s"https://www.surf-forecast.com/countries/${country.id}/regions.js"
    val doc = get(url)

    if ((doc >> element("img")).attr("alt").contains("Region")) {
      (doc >> elementList("option")).filter(_.attr("value").nonEmpty).map(e => Region(e.attr("value"), e.text))
    } else {
      Nil
    }
  }

}
