package com.pickard.metasurf.oneoffs

import com.pickard.metasurf.Entities.{Break, BreakDetails, BreakDetailsWebsiteUrl}
import com.pickard.metasurf.db.MongoDatabase
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import org.bson.conversions.Bson
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.mongodb.scala.{Completed, SingleObservable}
import org.mongodb.scala.model.Filters._
import com.pickard.metasurf.common.MyFuture._
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object MatchMSWBreaks {
  case class MSWSpotLookupResult(iso: String, name: String, hasNetcams: Boolean, URL: String, hasNearbyNetcams: Boolean, id: Int, score: Double)
  def main(args: Array[String]): Unit = {
    val mongoDB = new MongoDatabase
    val loadedBreaks = mongoDB.getCollection[Break]("surf-forecast-breaks")

    implicit val formats = DefaultFormats

    val lookupResults = loadedBreaks.find()
      .toFuture()
      .flatMap(f => Future.sequence(f.grouped(f.length / 8).map(breaks => Future {
        val maybeUrls: Seq[(String, Option[BreakDetailsWebsiteUrl])] = breaks.map(break => (break.name, lookupBreak(mongoDB)(break)))
        val successes = maybeUrls.collect({case (name, Some(url)) => url})
        val failures = maybeUrls.collect({case (name, None) => name})

        println(s"Found ${successes.length} of ${breaks.length}")

        Await.result(mongoDB.insert("break-details-website-urls", successes, ordered = false).toFuture, 20.seconds)
      }.dieQuietly)))

    val result = Await.result(lookupResults, Duration.Inf)

    val yes = result.collect({case Right(_) => 1})
    val no = result.collect({case Left(e) => e})

    println(s"there were ${yes.length} successes")
    println(s"there were ${no.length} failures")
    val noreasons = no.map(_.getMessage).mkString("\n")
    println(noreasons)
  }

  private def lookupBreak(mongoDB: MongoDatabase)(break: Break)(implicit formats: Formats): Option[BreakDetailsWebsiteUrl] = {
    val lookupUrl =
      s"https://magicseaweed.com/api/mdkey/search?limit=6&match=CONTAINS&&query=${break.name.toLowerCase().split(" ").mkString("%20")}"

    val result: String = scala.io.Source.fromURL(lookupUrl).mkString
    val json = parse(result)

    json match {
      case JArray(JObject(List(("type", JString("SPOT")), ("results", JArray(results)))) :: _) if results.nonEmpty =>
        val lookupResults = results.map(x => Serialization.read[MSWSpotLookupResult](compact(x)))
        val chosenResult = lookupResults.reduce((a,b) => if (a.score > b.score) a else b)
        ???
//        val websiteUrl = BreakDetailsWebsiteUrl(break.id, domain = "magicseaweed.com", infoUrl = )
//        Some(websiteUrl)
      case x =>
        None
    }
  }
}
