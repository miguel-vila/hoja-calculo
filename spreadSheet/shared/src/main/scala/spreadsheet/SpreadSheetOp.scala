package spreadsheet

import woot.{ Operation, SiteId }

case class SpreadSheetOp(cellId: CellId, op: Operation) {

  def from: SiteId = op.from
}
