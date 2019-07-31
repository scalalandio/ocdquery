package io.scalaland.ocdquery.internal

import scala.util.Random

object RandomName {

  def next: String = Random.alphanumeric.filter(_.isLetter).take(3).mkString.toLowerCase
}
