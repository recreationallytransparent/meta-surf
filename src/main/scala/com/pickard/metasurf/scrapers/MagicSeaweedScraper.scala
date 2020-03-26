package com.pickard.metasurf.scrapers
import com.pickard.metasurf.Entities
import java.time.format.DateTimeFormatter

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.pickard.metasurf.Entities
import com.pickard.metasurf.Entities._
import com.pickard.metasurf.common.Conversions
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

class MagicSeaweedScraper extends SurfScraper {
  override def domain: String = "magicseaweed.com"

  override def getForecast(id: String, doc: Document): Either[Throwable, Entities.BreakForecast] = {
    val dataRows: List[Element] = (doc >> elementList("tr")).filter(_.hasAttr("data-timestamp"))

    Try(dataRows.map(parseDataRow(id)(_))) match {
      case Success(l: List[BreakDetails]) => Right(BreakForecast(id, l))
      case Failure(e: Throwable) => Left(e)
    }
  }

  override def getInfo(id: String, doc: Document): Either[Throwable, Break] = ???

  private def parseDataRow(id: String)(tr: Element): BreakDetails = {
    // timestamp in seconds in html
    val dt = new DateTime(tr.attr("data-timestamp").toInt)

    lazy val tds = tr >> elementList("td")

    // msw uses 0-5 stars for rating, class name active means 1 star, inactive implies half a star
    val mswRating: Double = (tr >> elementList("td.table-forecast-rating li"))
      .map({
        case e: Element if e.attr("class").contains("active") => 1
        case e: Element if e.attr("class").contains("inactive") => 0.5
        case _ => 0
      })
      .sum
    // msw is 0-5, we use 0-100, make conversion here
    val score = Math.round(100 * (mswRating/5.0)).toInt

    // column indexes 3-5 contain primary swell information
    val primarySwellHeight: Double = tds.lift(3) match {
      case Some(e: Element) =>
        val heightPattern = "(^\\d*\\.?\\d+)(ft|m)".r
        e.text match {
          case heightPattern(h, units) if units == "ft" => Conversions.ft2m(h.toDouble)
          case heightPattern(h, units) if units == "m" => h.toDouble
          case _ => throw new RuntimeException(s"Could not parse swell height from ${e.text}")
        }
      case _ => throw new RuntimeException(s"Could not parse swell height because not enough tds in tr, there are ${tds.length} tds")
    }

    val primarySwellPeriod: Int = tds.lift(4) match {
      case Some(e: Element) => e.text.replace("s", "").toInt
      case _ => throw new RuntimeException(s"Could not parse swell period because not enough tds in tr, there are ${tds.length} tds")
    }

    val primarySwellDirection: Entities.Direction.Value = tds.lift(5) match {
      case Some(e: Element) if e.hasAttr("title") =>
        val directionPattern = "([A-Z]+) - ([0-9]+)°".r
        e.attr("title") match {
          case directionPattern(direction, angle) => Direction.withName(direction)
          case _ => throw new RuntimeException(s"Could not parse swell direction from ${e.attr("title")}")
        }
      case Some(_) => throw new RuntimeException("Could not parse swell direction because no element with title exists")
      case _ => throw new RuntimeException(s"Could not parse swell direction because not enough tds in tr, there are ${tds.length} tds")
    }

    //todo add secondary swell, same pattern as above with next 6 indicies (2nd and 3rd swells)
    //

    val windSpeed = (tr >> element("td.table-forecast-wind")) match {
      case e: Element =>
        val windPattern = "([0-9]+) ([0-9]+) (kph|mph)".r
        e.text match {
          case windPattern(windSpeed, gustSpeed, units) if units == "kph" => gustSpeed.toDouble
          case windPattern(windSpeed, gustSpeed, units) if units == "mph" => Conversions.mi2km(gustSpeed.toDouble)
          case _ => throw new RuntimeException(s"Could not parse windSpeed from ${e.text}")
        }
    }

    val directionPattern = "(.*), (.*) ([A-Z]+) - ([0-9]+)°".r
    val windDetailsTitle = tds.filter(_.hasAttr("title")).map(_.attr("title"))
    val windDetails = windDetailsTitle.collectFirst({
        case directionPattern(severity, state, direction, angle) => WindDetails(windSpeed, Direction.withName(direction), state)
    }) match {
      case Some(d) => d
      case _ =>
        throw new RuntimeException(s"Could not parse wind details because could not find element with title attribute adhering to expected regex. Instead title is ${windDetailsTitle}")
    }

    // todo: tides?
//    val tides =

    BreakDetails(id, dt, score, WaveDetails(primarySwellHeight, primarySwellDirection, primarySwellPeriod), windDetails, None, None)
  }
}
