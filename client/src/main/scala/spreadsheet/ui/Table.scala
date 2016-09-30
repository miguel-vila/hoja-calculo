package spreadsheet.ui

import org.scalajs.dom
import org.scalajs.dom.html
import dom.document
import org.scalajs.dom.raw.Element

object Table {

  def apply(
    m: Int,
    n: Int,
    columnText : Int => String,
    rowText : Int => String,
    tableElement : (Int, Int) => Element
  ) = {
    val table = document.createElement("table")
    table.classList.add("table-bordered")
    val thead = document.createElement("thead")
    table.appendChild(thead)
    val htr = document.createElement("tr")
    thead.appendChild(htr)
    val tbody = document.createElement("tbody")
    table.appendChild(tbody)
    htr.appendChild( document.createElement("th") )

    for { j <- (0 to n-1) } {
      htr.appendChild {
        val th = document.createElement("th")
        th.classList.add("border-cell")
        th.appendChild(document.createTextNode(columnText(j)))
        th
      }
    }

    for { i <- (0 to m-1) } {
      val tr = document.createElement("tr")
      val th = document.createElement("th")
      th.setAttribute("scope", "row")
      th.appendChild(document.createTextNode( rowText(i)  ))
      th.classList.add("border-cell")
      tr.appendChild(th)
      for { j <- (0 to n-1) } {
        val td = document.createElement("td")
        td.appendChild( tableElement(i,j) )
        tr.appendChild(td)
      }
      tbody.appendChild(tr)
    }
    table
  }

}
