package com.pickard.metasurf

import com.pickard.metasurf.Entities.{Break, Direction}
//import com.pickard.metasurf.db.InMemoryDatabase
import com.pickard.metasurf.scrapers.{MagicSeaweedScraper, SurfForecastScraper}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

object Main {
  def main(args: Array[String]): Unit = {
    println("hello world")
    import sext._
    val httpClient = new JsoupBrowser()
//    val database = new InMemoryDatabase(Map(
//      "taylors" -> Break("taylors", "Taylors-Mistake", "Taylor's Mistake", "NZ", (0,0), Direction.NNE, Direction.WSW)
//    ))
//    val scraper = new SurfForecastScraper()
//
//    val taylors = scraper.break("taylors", httpClient.get("https://surf-forecast.com/breaks/Taylors-Mistake/forecasts/latest"))
//    println(taylors.treeString)

//    val mswScraper = new MagicSeaweedScraper
//    val sumner = mswScraper.break("sumner-bar", httpClient.get("https://magicseaweed.com/Sumner-Bar-Christchurch-Surf-Report/1950/"))
//    sumner match {
//      case Right(x) => println(x.treeString)
//      case Left(e) => throw e
//    }
  }

}
