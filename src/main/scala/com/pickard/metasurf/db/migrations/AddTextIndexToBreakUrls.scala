package com.pickard.metasurf.db.migrations

import com.pickard.metasurf.db.{CollectionNames, MongoDatabase}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object AddTextIndexToBreakUrls extends LazyLogging {

  import org.mongodb.scala.model.Indexes._

  def main(args: Array[String]): Unit = {
    val mdb = new MongoDatabase
    val keys = "domain" :: "breakId" :: "infoUrl" :: Nil

    val i = compoundIndex(keys.map(text): _*)

    val future = mdb.getCollection(CollectionNames.breakUrls).createIndex(i).toFuture()


    future.onComplete({
      case Success(result) => logger.info(s"added index $result")
      case Failure(e) => logger.error("error adding index", e)
    })
    Await.result(future, Duration.Inf)
  }
}
