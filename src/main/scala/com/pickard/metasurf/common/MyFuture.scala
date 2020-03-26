package com.pickard.metasurf.common

import scala.concurrent.{ExecutionContext, Future}
import scala.util._

object MyFuture {
  implicit class RichFuture[T](future: Future[T]) {
    def dieQuietly(implicit context: ExecutionContext): Future[Either[Throwable, T]] = {
      future.transform({
        case Success(value) => Success(Right(value))
        case Failure(e) => Success(Left(e))
      })
    }
  }
}
