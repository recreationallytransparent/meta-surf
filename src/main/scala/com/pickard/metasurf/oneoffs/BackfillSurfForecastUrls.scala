package com.pickard.metasurf.oneoffs

import com.pickard.metasurf.Entities.{Break, BreakDetailsWebsiteUrl}
import com.pickard.metasurf.db.MongoDatabase
import org.mongodb.scala.{Completed, MongoCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object BackfillSurfForecastUrls {
  def main(args: Array[String]): Unit = {
    val mdb = new MongoDatabase
    val breaks: MongoCollection[Break] = mdb.getCollection[Break]("surf-forecast-breaks")

    import com.pickard.metasurf.common.MyFuture._
    val future: Future[Either[Throwable, Seq[Completed]]] = breaks.find()
      .map((break: Break) => {
        BreakDetailsWebsiteUrl(break.id, s"https://surf-forecast.com/breaks/${break.slug}/forecasts/latest", "surf-forecast.com")
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
