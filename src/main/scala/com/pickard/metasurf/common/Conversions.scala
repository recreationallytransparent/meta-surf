package com.pickard.metasurf.common

object Conversions {
  def ft2m(ft: Double): Double = ft / 3.28084

  def m2ft(m: Double): Double = m * 3.28084

  def mi2km(mi: Double): Double = mi * 1.60934

  def km2mi(km: Double): Double = km / 1.60934
}
