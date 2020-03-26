package com.pickard.metasurf.common
import java.io._

import scala.io.Source

object File {
  /**
    * write a `Seq[String]` to the `filename`.
    */
  def writeFile(filename: String)(lines: Seq[String]): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- lines) {
      bw.write(line)
    }
    bw.close()
  }

  def readFile(filename: String): Iterator[String] = Source.fromFile(filename).getLines
}
