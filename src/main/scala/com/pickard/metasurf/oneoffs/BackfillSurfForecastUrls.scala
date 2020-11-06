package com.pickard.metasurf.oneoffs

import com.pickard.metasurf.Entities.{Break, BreakDetailsWebsiteUrl}
import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.scrapers.JSoupDocumentProvider
import com.pickard.metasurf.scrapers.surfforecast.SurfForecastScraper
import org.mongodb.scala.{Completed, MongoCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object BackfillSurfForecastUrls {
  def main(args: Array[String]): Unit = {
    val mdb = new MongoDatabase
    val breaks: MongoCollection[Break] = mdb.getCollection[Break]("surf-forecast-breaks")
    val sfs = new SurfForecastScraper(new JSoupDocumentProvider)
    import com.pickard.metasurf.common.MyFuture._
    val future: Future[Either[Throwable, Seq[Completed]]] = breaks.find()
      .map((break: Break) => {
        BreakDetailsWebsiteUrl(break.id,
          infoUrl = sfs.infoUrl(break.slug),
          domain = sfs.domain,
          forecastsUrl = sfs.forecastUrl(break.slug),
          extendedForecastUrl = sfs.extendedForecastsUrl(break.slug)
        )
      })
      .flatMap(url => mdb.insert("break-details-website-urls", url))
      .toFuture()
      .dieQuietly

    import scala.concurrent.duration._
    val result = Await.result(future, 2.minutes)

    result match {
      case Left(e) => throw e
      case Right(l) => println(s"Success on ${l.length} inserts")
    }
  }
}
