package io.scalaland.ocdquery

import doobie.Fragment
import doobie.implicits._

sealed abstract class JoinType(val fragment: Fragment) extends Product with Serializable
object JoinType {
  case object Inner extends JoinType(fr"INNER")
  case object Left extends JoinType(fr"LEFT OUTER")
  case object Right extends JoinType(fr"RIGHT OUTER")
  case object Full extends JoinType(fr"FULL OUTER")
  case object Cross extends JoinType(fr"CROSS")
}
