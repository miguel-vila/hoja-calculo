package spreadsheet

import woot._

case class SpreadSheetContent(siteId: SiteId, name: String, content: Array[Array[WString]]) {

  def integrateOperation(cellOp: SpreadSheetOp): Unit = {
    val CellId(row,column) = cellOp.cellId
    val elem = content(row)(column)
    val (executedOps, newElem) = elem.integrate(cellOp.op)
    println(s"executedOps = ${executedOps.size}")
    content(row).update(column, newElem)
  }

  def withSiteId(otherSiteId: SiteId): SpreadSheetContent = {
    val newContent = content.map( row => row.map( _.copy(site = otherSiteId)  ) )
    new SpreadSheetContent(otherSiteId, name, newContent)
  }
}

object SpreadSheetContent {

  def apply(siteId: SiteId, name: String, n: Int, m: Int): SpreadSheetContent = {
    val content = Array.tabulate(m,n) { case _ =>
      WString.empty(siteId)
    }
    new SpreadSheetContent(siteId, name, content)
  }


}
