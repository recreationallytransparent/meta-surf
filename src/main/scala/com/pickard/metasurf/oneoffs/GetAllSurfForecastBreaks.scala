package com.pickard.metasurf.oneoffs

import com.pickard.metasurf.Entities.{Break, BreakDetailsWebsiteUrl}
import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.scrapers.SurfForecastScraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.{Document, Element}
import org.mongodb.scala.{Completed, SingleObservable}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util._

object GetAllSurfForecastBreaks {

  case class RegionLookupResult(id: String, name: String)

  case class BreakLookupResult(slug: String, name: String)

  def main(args: Array[String]): Unit = {
    val client = new JsoupBrowser()
    val baseDoc = client.get("https://www.surf-forecast.com/")
    val mongoClient = new MongoDatabase
    val scraper = new SurfForecastScraper

    val countryIds = (baseDoc >> elementList("#country_id option")).map(_.attr("value"))

    println(s"There are ${countryIds.length} countries to query")

    val allBreakSlugsFuture = Future.sequence(countryIds.map(cid => {
      Future {
        getRegions(client)(cid) match {
          case Left(regions) => regions.flatMap(r => getBreaks(client)(r.id))
          case Right(breaks) => breaks
        }
      }
    }))
      .map(_.flatten)

    val allBreakSlugs = Await.result(allBreakSlugsFuture, 2.minutes)

    println(s"There are ${allBreakSlugs.length} break slugs")

    // now for each break, get the break info
    val insertResults: List[Future[List[Either[Throwable, Seq[Completed]]]]] = allBreakSlugs
      .grouped(20)
      .toList
      .zipWithIndex
      .map({
        case (group: Seq[BreakLookupResult], index) => Future {
          println(s"Start group $index of ${allBreakSlugs.length / 20}")
          (group.map(x => scraper.getInfo(x.slug, client.get(detailsUrl(x)))), index)
        }
      })
      .map((future: Future[(List[Either[Throwable, Break]], Int)]) => {
        future.flatMap({
          case (breakScrapeResults: List[Either[Throwable, Break]], groupIndex) =>
            val successes = breakScrapeResults.collect({ case Right(break) => break })
            val failures = breakScrapeResults.collect({ case Left(error) => error })

            println(s"There are ${successes.length} successes and ${failures.length}, failures in group $groupIndex")
            failures.foreach(_.printStackTrace())

            val insert =
              mongoClient.insert("surf-forecast-breaks", successes, ordered = false)
              .flatMap(_ => mongoClient.insert("break-details-website-urls", successes.map(b => {
                BreakDetailsWebsiteUrl(b.id, detailsUrl(b), "surf-forecast.com")
              }), ordered = false))
                .toFuture
              .transform({
                case Success(x) =>
                  Success(Right(x))
                case Failure(e) => Success(Left(e))
              })

            // preserve errors
            val insertsAndFailures = failures.map(e => Future.successful(Left(e))) :+ insert

            Future.sequence(insertsAndFailures)
        })
      })


    val results: List[List[Either[Throwable, Seq[Completed]]]] = Await.result(Future.sequence(insertResults), 30.minutes)

    val failCount = results.flatten.count(_.isLeft)
    val successCount = results.flatten.count(_.isRight)

    println(s"Inserted $successCount, failed to insert: $failCount")

    results.flatten.collect({ case Left(e) =>
      println("throwing error for address")
      throw e
    })
  }

  private def detailsUrl(lookupResult: BreakLookupResult) = s"https://surf-forecast.com/breaks/${lookupResult.slug}"
  private def detailsUrl(break: Break) = s"https://surf-forecast.com/breaks/${break.slug}"

  def getRegions(client: JsoupBrowser)(countryId: String): Either[List[RegionLookupResult], List[BreakLookupResult]] = {
    val url = s"https://www.surf-forecast.com/countries/$countryId/regions.js"
    val doc = client.get(url)

    if ((doc >> element("img")).attr("alt").contains("Region")) {
      Left(parseRegions(doc >> element("#region_id")))
    } else {
      Right(parseBreaks(doc >> element("#location_filename_part")))
    }
  }

  def getBreaks(client: JsoupBrowser)(regionId: String): List[BreakLookupResult] = {
    val url = s"https://www.surf-forecast.com/regions/$regionId/breaks.js"
    parseBreaks(client.get(url) >> element("#location_filename_part"))
  }

  def parseRegions(doc: Element): List[RegionLookupResult] = {
    (doc >> elementList("option")).filter(_.attr("value").nonEmpty).map(e => RegionLookupResult(e.attr("value"), e.text))
  }

  def parseBreaks(doc: Element): List[BreakLookupResult] = {
    (doc >> elementList("option")).filter(_.attr("value").nonEmpty).map(e => BreakLookupResult(e.attr("value"), e.text))
  }
}
