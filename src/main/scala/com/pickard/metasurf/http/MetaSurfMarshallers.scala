package com.pickard.metasurf.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.pickard.metasurf.Entities
import com.pickard.metasurf.Entities._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

trait MetaSurfMarshallers {
  implicit val datetimeFormat: RootJsonFormat[DateTime] = new RootJsonFormat[DateTime] {
    private val formatter = ISODateTimeFormat.dateTimeNoMillis()

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => formatter.parseDateTime(s)
      case x => throw DeserializationException(s"Could not parse $x into DateTime")
    }

    override def write(obj: DateTime): JsValue = JsString(formatter.print(obj))
  }

  implicit val directionFormat: RootJsonFormat[Entities.Direction.Value] = new RootJsonFormat[Direction.Value] {
    override def read(json: JsValue): Entities.Direction.Value = json match {
      case JsString(txt) => Direction.withName(txt)
      case x => throw DeserializationException(s"Can only read strings into Direction, passed: [$x]")
    }

    override def write(obj: Entities.Direction.Value): JsValue = {
      JsString(obj.toString)
    }
  }

  implicit val breakFormat: RootJsonFormat[Break] = jsonFormat9(Break)
  implicit val breakResponseFormat: RootJsonFormat[BreaksResponse] = jsonFormat1(BreaksResponse)
  implicit val waveDetailsFormat: RootJsonFormat[WaveDetails] = jsonFormat3(WaveDetails)
  implicit val windDetailsFormat: RootJsonFormat[WindDetails] = jsonFormat3(WindDetails)
  implicit val breakDetailsFormat: RootJsonFormat[BreakDetails] = jsonFormat7(BreakDetails)
  implicit val breakForecastFormat: RootJsonFormat[BreakForecast] = jsonFormat2(BreakForecast)
  implicit val metaForecastFormat: RootJsonFormat[MetaForecast] = jsonFormat1(MetaForecast)
  implicit val searchResultsFormat: RootJsonFormat[SearchResults] = jsonFormat2(SearchResults)
  implicit val countryFormat: RootJsonFormat[Country] = jsonFormat2(Country)
  implicit val regionFormat: RootJsonFormat[Region] = jsonFormat2(Region)
  implicit val countryRegionsFormat: RootJsonFormat[CountryRegions] = jsonFormat2(CountryRegions)
  implicit val countryRegionsResponseFormat = jsonFormat1(CountryRegionsResponse)
  implicit val breakDetailsWebsiteUrlFormat = jsonFormat5(BreakDetailsWebsiteUrl)
  implicit val breakDetailsWebsiteUrlSearchResultFormat = jsonFormat1(BreakDetailsWebsiteUrlSearchResult)
}
