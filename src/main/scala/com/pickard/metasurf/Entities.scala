package com.pickard.metasurf

import org.joda.time.DateTime

object Entities {
  object Direction extends Enumeration {
    type Direction = Value

    val N = Value("N")
    val NNE = Value("NNE")
    val NE = Value("NE")
    val ENE = Value("ENE")

    val E = Value("E")
    val ESE = Value("ESE")
    val SE = Value("SE")
    val SSE = Value("SSE")

    val S = Value("S")
    val SSW = Value("SSW")
    val SW = Value("SW")
    val WSW = Value("WSW")

    val W = Value("W")
    val WNW = Value("WNW")
    val NW = Value("NW")
    val NNW = Value("NNW")
  }

  import Entities.Direction._

  case class Break(id: String,
                   slug: String,
                   name: String,
                   region: String,
                   country: String,
                   lngLat: (Double, Double),
                   style: String,
                   bestSwell: Direction,
                   bestWind: Direction)
  case class BreaksResponse(breaks: Iterable[Break])

  case class WaveDetails(swellHeightM: Double, swellDirection: Direction, period: Int)

  case class WindDetails(windKph: Double, windDirection: Direction, windState: String)

  case class BreakDetails(breakId: String,
                          dateTime: DateTime,
                          score: Int,
                          waveDetails: WaveDetails,
                          windDetails: WindDetails,
                          highTide: Option[DateTime],
                          lowTide: Option[DateTime])

  case class BreakForecast(breakId: String, forecast: List[BreakDetails])

  case class BreakDetailsWebsiteUrl(breakId: String, infoUrl: String, domain: String, forecastsUrl: String, extendedForecastUrl: String)
  case class BreakDetailsWebsiteUrlSearchResult(results: Iterable[BreakDetailsWebsiteUrl])

  case class MetaForecast(forecasts: List[(String, BreakForecast)])

  case class SearchResults(query: String, breaks: List[Break])

  case class Country(id: String, name: String)
  case class Region(id: String, name: String)
  case class CountryRegions(country: Country, regions: List[Region])
  case class CountryRegionsResponse(countryRegions: List[CountryRegions])
}
