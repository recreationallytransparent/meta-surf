package com.pickard.metasurf.db


trait MongoSurfSelectors  {
  import org.mongodb.scala.model.Filters._

  def breaksByCountryRegion(countryName: String, regionName: String) =
    and(equal("country", countryName), equal("region", regionName))
}