package com.pickard.metasurf.controllers

import com.pickard.metasurf.Entities.CountryRegions
import com.pickard.metasurf.db.{CollectionNames, MongoDatabase}

class CountryRegionsController(mongo: MongoDatabase) extends CollectionController[CountryRegions](mongo, CollectionNames.countryRegions) {
}
