package spreadsheet

import CellId._

case class CellId(
  row: Int,
  column: Int
) {

  override def toString(): String = s"${columnChar(column)}$row"

}

object CellId {

  def columnChar(n: Int): Char = ('A'+n).toChar

}
