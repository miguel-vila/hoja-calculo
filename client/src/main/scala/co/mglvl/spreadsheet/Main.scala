package co.mglvl.spreadsheet

import co.mglvl.spreadsheet.ui.Spreadsheet
import co.spreadsheet._

import scala.scalajs.js
import org.scalajs.dom
import dom._
import woot._
import upickle.default._

object Main extends js.JSApp {


  def main(): Unit = {
    val cellsElem = document.getElementById("cells")
    val ws = new dom.WebSocket("ws://localhost:8082/ws/edit/abc")
    def broadcastCellOperation(cellOp: SpreadSheetOp): Unit = {
      ws.send( write(cellOp) )
    }
    ws.onmessage = { x: MessageEvent =>
      println(s"received = ${x.data.toString}")
      val sp = read[SpreadSheetContent](x.data.toString)
      println(s"site id = ${sp.siteId}")
      val spreadSheet = Spreadsheet(sp, cellsElem, broadcastCellOperation)
    }

  }

}
