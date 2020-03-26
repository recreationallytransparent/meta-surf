package com.pickard.metasurf.scrapers

import com.pickard.metasurf.Entities.{Break, BreakForecast}
import net.ruippeixotog.scalascraper.model.Document

trait SurfScraper {
  def getForecast(id: String, doc: Document): Either[Throwable, BreakForecast]
  def getInfo(id: String, doc: Document): Either[Throwable, Break]
  def domain: String
}
