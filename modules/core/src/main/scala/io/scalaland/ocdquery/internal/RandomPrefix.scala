package io.scalaland.ocdquery.internal

import scala.util.Random

object RandomPrefix {

  @inline def next: String = Random.alphanumeric.filter(_.isLetter).take(3).mkString.toLowerCase
}
