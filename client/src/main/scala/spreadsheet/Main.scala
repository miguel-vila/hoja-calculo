package spreadsheet

import spreadsheet.ui.Spreadsheet
import spreadsheet._

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
      println(s"broadcasting $cellOp")
      ws.send( write(cellOp) )
    }
    var spreadSheet: Spreadsheet = null
    ws.onmessage = { x: MessageEvent =>
      read[ClientMessage](x.data.toString) match {
        case ClientMessage(Some(sp),_) =>
          println(s"site id = ${sp.siteId}")
          spreadSheet = Spreadsheet(sp, cellsElem, broadcastCellOperation)
        case ClientMessage(_,Some(operation)) =>
          if(operation.from != spreadSheet.siteId) { //@TODO this filtering should be done from the server
            println(s"received operation $operation")
            spreadSheet.receiveRemoteOperation(operation)
          }
      }
    }

  }

}
