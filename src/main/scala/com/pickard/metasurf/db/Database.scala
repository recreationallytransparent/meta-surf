package com.pickard.metasurf.db

import com.pickard.metasurf.Entities.Break

trait Database {
  def write[T](table: String, collection: Iterable[T]): Either[Throwable, Unit]
  def read[T](table: String)(transform: Array[String] => T): Iterable[T]
  def find[T](id: String): Option[T]
}
