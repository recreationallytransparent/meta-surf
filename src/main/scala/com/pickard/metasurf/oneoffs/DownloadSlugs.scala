package com.pickard.metasurf.oneoffs

import java.io.{BufferedWriter, File, FileWriter}

import com.pickard.metasurf.db.MongoDatabase
import com.pickard.metasurf.scrapers.surfforecast.SurfForecastScraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization

import scala.concurrent.Await

object DownloadSlugs {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      throw new IllegalArgumentException("must provide dest file");
    }

    val dest = args.head
    val client = new JsoupBrowser()

    import scala.concurrent.duration._
    val slugs = Await.result(GetAllSurfForecastBreaks.getSlugs(client), 5.minutes)

    val f = new File(dest)
    val bw = new BufferedWriter(new FileWriter(f))
    bw.write(Serialization.write(slugs)(DefaultFormats))
    bw.close()
    println(s"wrote ${slugs.length} to ${f.getAbsolutePath}")
  }
}
