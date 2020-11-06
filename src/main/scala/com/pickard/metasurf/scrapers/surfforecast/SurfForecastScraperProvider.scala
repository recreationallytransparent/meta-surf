package com.pickard.metasurf.scrapers.surfforecast

import com.pickard.metasurf.Entities._
import com.pickard.metasurf.scrapers.surfforecast.SurfForecastScraperProvider.SurfForecastWaveStats
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model._
import org.joda.time.DateTime
import org.json4s._
import org.json4s.jackson.Serialization

import scala.util._

object SurfForecastScraperProvider {

  case class SurfForecastWaveStats(period: String, angle: String, dir: String, height: String, sheltered: Boolean, color: String)

}

trait SurfForecastScraperProvider {
  implicit val formats: Formats = DefaultFormats

  protected def parseTimes(timeRow: Element): List[DateTime] = {
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

  protected def parseRating(row: Element): List[Double] = {
    (row >> elementList("td") >> attr("alt")("img")).map(_.toDouble)
  }

  protected def parseWaves(row: Element): List[WaveDetails] = {
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
        .reduce((a, b) => if (a.swellHeightM >= b.swellHeightM) a else b)
    })
  }

  protected def parseWind(windRow: Element, windStateRow: Element): List[WindDetails] = {
    val winds = (windRow >> elementList("td")).map(_.text)
    val windStates = (windStateRow >> elementList("td")).map(_.text)

    winds.zipWithIndex.map({
      case (s, i) =>
        s.split(" ") match {
          case Array(speed, dir) => WindDetails(speed.toDouble, Direction.withName(dir), windStates(i))
        }
    })
  }

  protected def parseTides(row: Element, columnDates: List[DateTime]): List[(DateTime, Int)] = {
    // use the index as a pointer to the date, because the string in each td only denotes the time
    val tideStrings: List[(String, Int)] = (row >> elementList("td"))
      .map(e => e.text)
      .zipWithIndex
      .filter(s => s._1.nonEmpty)

    val tideRegex = "([0-9]+):([0-9]+)(AM|PM) -?[0-9]+.[0-9]+".r
    tideStrings
      .map(t => (t._1, columnDates(t._2), t._2))
      .map({
        case (tideRegex(hour, minute, ampm), date, i) =>
          val hourampm = if (ampm == "PM") {
            if (hour.toInt + 12 == 24) 0
            else hour.toInt + 12
          } else {
            hour.toInt
          }
          val dt = new DateTime().withDate(date.toLocalDate).withTime(hourampm, minute.toInt, 0, 0)
          (dt, i)
        case (s, d, i) => throw new Exception(s"Could not understand ${s} as high/low tide time")
      })
  }
}
