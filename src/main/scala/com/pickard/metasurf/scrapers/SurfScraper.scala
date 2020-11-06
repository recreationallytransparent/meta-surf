package com.pickard.metasurf.scrapers

import com.pickard.metasurf.Entities.{Break, BreakForecast, CountryRegions}
import net.ruippeixotog.scalascraper.model.Document

trait SurfScraper extends DocumentProvider {
  def getForecast(id: String, doc: Document): Either[Throwable, BreakForecast]
  def getInfo(id: String, doc: Document): Either[Throwable, Break]
  def domain: String
  def baseUrl: String
  def infoUrl(slug: String): String
  def forecastUrl(slug: String): String
  def extendedForecastsUrl(slug: String): String
  def getCountryRegions: Either[Throwable, List[CountryRegions]]
}
