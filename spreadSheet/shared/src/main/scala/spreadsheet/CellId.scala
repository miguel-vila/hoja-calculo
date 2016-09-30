package spreadsheet

import CellId._

case class CellId(
  row: Int,
  column: Int
) {

  override def toString(): String =
    s"${columnChar(column)}$row"

  def <=(other: CellId): Boolean =
    row <= other.row && column <= other.column

}

object CellId {

  def columnChar(n: Int): Char = ('A'+n).toChar

}
