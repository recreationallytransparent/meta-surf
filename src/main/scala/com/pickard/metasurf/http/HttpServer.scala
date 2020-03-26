package com.pickard.metasurf.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http

import scala.io.StdIn
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import Marshallers._
import com.pickard.metasurf.Entities
import com.pickard.metasurf.controllers.{BreakController, ForecastController}
import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.scrapers.{MagicSeaweedScraper, SurfForecastScraper}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.concurrent.Future

object HttpServer {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("meta-surf")
    implicit val mat = ActorMaterializer()
    implicit val ctxt = system.dispatcher

    val browser = new JsoupBrowser()
    val mdb = new MongoDatabase
    val scrapers = new MagicSeaweedScraper :: new SurfForecastScraper :: Nil

    val forecastController = new ForecastController(browser, mdb, scrapers)
    val breakController = new BreakController(mdb)

    val route =
      path("forecast" / Remaining) { id =>
        get {
          val maybeItem: Future[Option[Entities.MetaForecast]] = forecastController.forId(id)
          onSuccess(maybeItem) {
            case Some(item) => complete(item)
            case None => complete(StatusCodes.NotFound)
          }
        }
      } ~ path("break" / Remaining) { breakId =>
        get {
          val maybeBreak = breakController.break(breakId)
          onSuccess(maybeBreak) {
            case Some(item) => complete(item)
            case _ => complete(StatusCodes.NotFound)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
