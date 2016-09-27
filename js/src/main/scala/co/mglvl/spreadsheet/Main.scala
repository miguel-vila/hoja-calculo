package co.mglvl.spreadsheet

import co.mglvl.spreadsheet.ui.Spreadsheet

import scala.scalajs.js
import org.scalajs.dom
import dom.document

object Main extends js.JSApp {

  def main(): Unit = {
    val cellsElem = document.getElementById("cells")
    val m = 5
    val n = 5
    val spreadSheet = Spreadsheet(m, n, cellsElem)
    println("Listo!!")
  }

}
