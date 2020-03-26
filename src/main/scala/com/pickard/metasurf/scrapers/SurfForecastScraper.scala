package com.pickard.metasurf.scrapers

import java.time.format.DateTimeFormatter

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.pickard.metasurf.Entities
import com.pickard.metasurf.Entities._
import com.pickard.metasurf.db.Database
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.joda.time.DateTime
import org.json4s.jackson.Serialization

import scala.util.matching.Regex
import scala.util._

object SurfForecastScraper {
  case class SurfForecastWaveStats(period: String, angle: String, dir: String, height: String, sheltered: Boolean, color: String)

}
class SurfForecastScraper() extends SurfScraper {
  import SurfForecastScraper._

  private val baseUrl: String = "https://www.surf-forecast.com"
  implicit val formats: Formats = DefaultFormats

/*  override def break(breakId: String): Option[BreakForecast] = {
    for (
      b <- breakDatabase.getBreakForId(breakId)
    ) yield {
      getForecast(b) match {
        case Failure(exception) => throw exception
        case Success(forecast) => forecast
      }
    }
  }

  private def breakUrl(break: Break): String = {
    baseUrl + "/breaks/" + break.slug + "/forecasts/latest"
  }

  private def getForecast(break: Break): Try[BreakForecast] = {
    val url = breakUrl(break)
    val document: Document = client.get(url)
    val srcTable = document >> element("table")
    println(srcTable)
    ???
  }*/

  override def domain: String = "surf-forecast.com"

  override def getForecast(id: String, doc: Document): Either[Throwable, BreakForecast] = {
    val dataMap: Map[String, Element] = (doc >> elementList("table tr"))
      .filter(_.attr("data-row-name").nonEmpty)
      .map(r => r.attr("data-row-name") -> r)
      .toMap

    val maybeForecast: Option[BreakForecast] = for (
      times <- dataMap.get("time").map(parseTimes);
      rating <- dataMap.get("rating").map(parseRating);
      waves <- dataMap.get("wave-height").map(parseWaves);
      windState <- dataMap.get("wind-state");
      wind <- dataMap.get("wind").map(x => parseWind(x, windState));
      highTide <-  dataMap.get("high-tide").map(x => parseTides(x, times));
      lowTide <- dataMap.get("low-tide").map(x => parseTides(x, times))
    ) yield {
      val forecast = (times.zipWithIndex zip rating zip waves zip wind).map(t => (t._1._1._1._1, t._1._1._1._2, t._1._1._2, t._1._2, t._2)).map({
        case (time, timeIndex, rating, waveDetails, windDetails) =>
          val hTide = highTide.find(_._2 == timeIndex).map(_._1)
          val lTide = lowTide.find(_._2 == timeIndex).map(_._1)
          BreakDetails(id, time, Math.round(100 * (rating/10)).toInt, waveDetails, windDetails, hTide, lTide)
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

  /**
    * <td class="forecast-table__cell forecast-table-time__cell">
    *   <span class="forecast-table__value">10</span> <span class="forecast-table__value">AM</span>
    *   </td>
   */
  private def parseTimes(timeRow: Element): List[DateTime] = {
    // because we don't have more information outside of day of week and day of month and time, we have to assume
    // that the first day is today
    val thisInstant = DateTime.now()
    val today = new DateTime()
      .withDate(thisInstant.getYear, thisInstant.getMonthOfYear, thisInstant.getDayOfMonth)
      .withTime(0, 0, 0, 0)

//    val days: List[Element] = (dayRow >> elementList("td")).filter(_.attr("data-day-name").nonEmpty)
    val hours: List[String] = (timeRow >> elementList("td.forecast-table-time__cell") >> elementList("span")).map(x => x.map(_.text).mkString(" "))

    val pattern = "([0-9]+)? ?(AM|PM|Night)".r
    val hourOfDay = hours
      .map({
        case pattern(hour, tod) =>
          List(hour, tod).filter(_ != null)
      })
      .map({
        case "AM" :: Nil => 4
        case "PM" :: Nil => 12
        case "Night" :: Nil => 17
        case i :: "AM" :: Nil => i.toInt
        case i :: "PM" :: Nil => i.toInt + 12
        case x => throw new Exception(s"Can not determine hour of day given ${x}")
      })

    def loop(l: DateTime, i: List[Int], r: List[DateTime] = Nil): List[DateTime] = {
      if (i.isEmpty) r
      else {
        val ihead = i.head
        val lh = l.getHourOfDay
        val addHours = if (ihead > lh) ihead - lh else 24 - lh + ihead
        val c: DateTime = l.plusHours(addHours)
        loop(c, i.tail, r :+ c)
      }
    }

    loop(today, hourOfDay)
  }

//  def parseTimes(row: Element): List[DateTime] = {
//    val today = DateTime.now()
//    val pattern = "([A-Z][a-z]+) ([0-9]+) ([0-9]+AM|PM)".r
//
//    (row >> elementList("td")).map(_.attr("data-date")).map(s => {
//      val (dayOfMonth, timeOfDay) = s match {
//        case pattern(_, dayOfM, timeOfDayString) if timeOfDayString.contains("AM") =>
//          (dayOfM.toInt, timeOfDayString.replace("AM", "").toInt)
//
//        case pattern(_, dayOfM, timeOfDayString) if timeOfDayString.contains("PM") =>
//          (dayOfM.toInt, timeOfDayString.replace("PM", "").toInt + 12)
//
//        case _ => throw new Exception("Could not parse")
//      }
//
//      new DateTime()
//        .withDate(today.getYear, today.getMonthOfYear, dayOfMonth)
//        .withTime(timeOfDay, 0, 0, 0)
//    })
//  }

  private def parseRating(row: Element): List[Double] = {
    (row >> elementList("td") >> attr("alt")("img")).map(_.toDouble)
  }

  private def parseWaves(row: Element): List[WaveDetails] = {
    val jsons = (row >> elementList("td")).map(_.attr("data-swell-state")).filter(_.nonEmpty)
    jsons.map(json => {
      val sfws = Serialization.read[Array[SurfForecastWaveStats]](json)
      sfws.map(s => Try(WaveDetails(
        swellHeightM = s.height.toDouble,
        swellDirection = Direction.withName(s.dir),
        period = s.period.toInt
      )))
        .collect({
          case Success(x) => x
        })
        // todo handle different swells, instead of just major
        .reduce((a,b) => if (a.swellHeightM >= b.swellHeightM) a else b)
    })
  }

  private def parseWind(windRow: Element, windStateRow: Element): List[WindDetails] = {
    val winds = (windRow >> elementList("td")).map(_.text)
    val windStates = (windStateRow >> elementList("td")).map(_.text)

    winds.zipWithIndex.map({
      case (s, i) =>
        s.split(" ") match {
          case Array(speed, dir) => WindDetails(speed.toDouble, Direction.withName(dir), windStates(i))
        }
    })
  }

  private def parseTides(row: Element, columnDates: List[DateTime]): List[(DateTime, Int)] = {
    // use the index as a pointer to the date, because the string in each td only denotes the time
    val tideStrings: List[(String, Int)] = (row >> elementList("td"))
      .map(e => e.text)
      .zipWithIndex
      .filter(s => s._1.nonEmpty)

    val tideRegex = "([0-9]+):([0-9]+)(AM|PM) [0-9]+.[0-9]+".r
    tideStrings
      .map(t => (t._1, columnDates(t._2), t._2))
      .map({
        case (tideRegex(hour, minute, ampm), date, i) =>
          val hourampm = if (ampm == "PM") hour.toInt + 12 else hour.toInt
          val dt = new DateTime().withDate(date.toLocalDate).withTime(hourampm, minute.toInt, 0, 0)
          (dt, i)
        case (s, d, i) => throw new Exception(s"Could not understand ${s} as high/low tide time")
      })
  }
}
