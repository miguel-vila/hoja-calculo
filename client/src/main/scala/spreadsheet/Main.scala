package spreadsheet

import spreadsheet.ui.Spreadsheet
import spreadsheet._

import scala.scalajs.js
import org.scalajs.dom
import dom._
import woot._
import upickle.default._

import scala.scalajs.js.timers._
import scala.concurrent.duration._

object Main extends js.JSApp {

  def main(): Unit = {
    val cellsElem = document.getElementById("cells")
    val connection = new SpreadsheetConnection(cellsElem)
  }

}
