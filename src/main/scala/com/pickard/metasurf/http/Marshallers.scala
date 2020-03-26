package com.pickard.metasurf.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.pickard.metasurf.Entities
import com.pickard.metasurf.Entities._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

object Marshallers {
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
  implicit val waveDetailsFormat: RootJsonFormat[WaveDetails] = jsonFormat3(WaveDetails)
  implicit val windDetailsFormat: RootJsonFormat[WindDetails] = jsonFormat3(WindDetails)
  implicit val breakDetailsFormat: RootJsonFormat[BreakDetails] = jsonFormat7(BreakDetails)
  implicit val breakForecastFormat: RootJsonFormat[BreakForecast] = jsonFormat2(BreakForecast)
  implicit val metaForecastFormat: RootJsonFormat[MetaForecast] = jsonFormat1(MetaForecast)
}
