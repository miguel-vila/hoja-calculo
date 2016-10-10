package spreadsheet

import scala.scalajs.js
import org.scalajs.dom
import dom._

object Main extends js.JSApp {

  def main(): Unit = {
    val cellsElem = document.getElementById("cells")
    val connection = new SpreadsheetConnection(cellsElem)
  }

}
