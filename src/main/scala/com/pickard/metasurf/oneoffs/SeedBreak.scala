package com.pickard.metasurf.oneoffs

import java.io.{File, FileReader}

import com.pickard.metasurf.Entities
import com.pickard.metasurf.Entities.BreakDetailsWebsiteUrl
import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.oneoffs.GetAllSurfForecastBreaks.BreakLookupResult
import com.pickard.metasurf.oneoffs.SeedBreak.SeedBreakArguments
import com.pickard.metasurf.common.strings.RatcliffObershelpMetric
import com.pickard.metasurf.scrapers.JSoupDocumentProvider
import com.pickard.metasurf.scrapers.surfforecast.SurfForecastScraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import org.mongodb.scala.Completed
import org.rogach.scallop.ScallopConf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util._

object SeedBreak {
  class SeedBreakArguments(args: Array[String]) extends ScallopConf(args) {
    val break = opt[String](name = "break", required = true, descr = "NAme of the break to query against")
    val slugsLocation = opt[String](name = "slugsLocation", required = false, descr = "Include if slugs exist on disk", default = None)
    verify()
  }

  def main(args: Array[String]): Unit = {
    new SeedBreak(new SeedBreakArguments(args)).go()
  }
}

class SeedBreak(args: SeedBreakArguments) {
  private def loadSlugs(loc: String): List[BreakLookupResult] = {
    val src: String = Source.fromResource(loc).getLines.mkString("\n")
    implicit val formats = DefaultFormats
    Serialization.read[List[BreakLookupResult]](src)
  }

  def go(): Unit = {
    val client = new JsoupBrowser()
    val baseDoc = client.get("https://www.surf-forecast.com/")
    val mongoClient = new MongoDatabase
    val scraper = new SurfForecastScraper(new JSoupDocumentProvider)

    import scala.concurrent.duration._
    val slugs = args.slugsLocation.toOption.map(loadSlugs).getOrElse(Await.result(GetAllSurfForecastBreaks.getSlugs(client), 5.minutes))

    val potentialSlugs: List[(BreakLookupResult, Double)] = slugs.par.map(slug => (slug, RatcliffObershelpMetric.compare(slug.name.toLowerCase(), args.break())))
      .flatMap(t => t._2.filter(_ > 0.5).map((t._1, _)))
      .toList
      .sortBy(_._2)(Ordering[Double].reverse)

    println(s"Found ${potentialSlugs.length}, with highest similarity at ${potentialSlugs.headOption.map(_._2).getOrElse(0.0)}")

    if (potentialSlugs.headOption.exists(_._2 >= 0.75)) {
      val slug = potentialSlugs.head._1
      val break = scraper.getInfo(slug.slug, client.get(scraper.infoUrl(slug.slug))) match {
        case Left(e) => throw e
        case Right(break) => break
      }

      return ???
//      val urlEntry = BreakDetailsWebsiteUrl(break.id, scraper.infoUrl(break.slug), scraper.domain)
//
//      val f1 = mongoClient.insert("surf-forecast-breaks", break).toFuture()
//      val f2 = mongoClient.insert("break-details-website-urls", urlEntry).toFuture()
//
//      val f = Future.sequence(f1 :: f2 :: Nil)
//
//      f.onComplete({
//        case Success(_) => println(s"successfully inserted break and url for ${break.id}-${break.name}")
//        case Failure(e) => throw e
//      })
//
//      Await.result(f, 5.seconds)
    } else {
      throw new RuntimeException(s"Could not find slug that matches ${args.break()}, list of close matches: ${potentialSlugs.map(t => s"${t._1.name} - ${t._2}").mkString("\n")}")
    }
  }
}
