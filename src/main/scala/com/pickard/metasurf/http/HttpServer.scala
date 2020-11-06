package com.pickard.metasurf.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http

import scala.io.StdIn
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.pickard.metasurf.Entities
import com.pickard.metasurf.Entities.{BreakDetailsWebsiteUrl, BreakDetailsWebsiteUrlSearchResult, BreaksResponse, CountryRegionsResponse}
import com.pickard.metasurf.controllers.{BreakController, BreakUrlController, CountryRegionsController, ForecastController}
import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.scrapers.{JSoupDocumentProvider, MagicSeaweedScraper}
import com.pickard.metasurf.scrapers.surfforecast.SurfForecastScraper
import com.typesafe.scalalogging.LazyLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.concurrent.Future

object HttpServer extends CORSHandler with MetaSurfMarshallers with LazyLogging {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("meta-surf")
    implicit val mat = ActorMaterializer()
    implicit val ctxt = system.dispatcher

    val browser = new JsoupBrowser()
    val mdb = new MongoDatabase
    val scrapers = new MagicSeaweedScraper(new JSoupDocumentProvider) :: new SurfForecastScraper(new JSoupDocumentProvider) :: Nil

    val forecastController = new ForecastController(browser, mdb, scrapers)
    val breakController: BreakController = new BreakController(mdb)
    val countryRegionsController = new CountryRegionsController(mdb)
    val breakUrlController = new BreakUrlController(mdb)

    val route = {
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
      } ~ path("countryRegions") {
        get {
          onSuccess(countryRegionsController.get()) {
            case x => complete {
              CountryRegionsResponse(x.toList)
            }
          }
        }
      } ~ path("countryRegions" / "breaks") {
        parameters('countryName, 'regionName) { (countryName, regionName) =>
          logger.info(s"GET $countryName $regionName")
          onSuccess(breakController.byCountryRegion(countryName, regionName)) { items =>
            logger.info(s"received $items")
            complete(BreaksResponse(items))
          }
        }
      }
    } ~ pathPrefix("admin") {
      pathPrefix("break-urls") {
        get {
          parameters('search) { (search) =>
            onSuccess(breakUrlController.search(search)) { items =>
              logger.info(s"Search for $search yields ${items.size}")
              complete(BreakDetailsWebsiteUrlSearchResult(items))
            }
          }
        } ~ post {
          logger.info("Try put url")
          entity(as[BreakDetailsWebsiteUrl]) { item =>
            onSuccess(breakUrlController.add(item)) { result =>
              logger.info(s"added $item")
              complete(result)
            }
          }
        }
      }
    } /*~ path("search") {

        parameter('q) { queryString =>
          val r = breakController.search(queryString)
          onSuccess(r) {
            case searchResult => complete(searchResult)
          }
        }
      }*/

    val bindingFuture = Http().bindAndHandle(corsHandler(route), "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
