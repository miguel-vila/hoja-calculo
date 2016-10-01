package spreadsheet

import spreadsheet.ui.Spreadsheet
import spreadsheet._
import scala.collection.mutable.Queue
import org.scalajs.dom
import upickle.default._
import dom._

class SpreadsheetConnection(
  root: Element
) {

  private var ws: WebSocket = _
  private var connected = false
  //private val pendingOps = Queue.empty[SpreadSheetOp]
  private var spreadSheet: Spreadsheet = null
  val MAX_RECONNECTIONS = 5

  connect()

  private def connectingMsgClasses =
    document
      .getElementById("connectingmessage")
      .classList

  private def showConnectingMsg() = connectingMsgClasses.remove("hidden")

  private def hideConnectingMsg() = connectingMsgClasses.add("hidden")

  private def connect(): Unit = {
    val protocol = if( window.location.protocol.startsWith("https") ) { "wss" } else { "ws" }
    showConnectingMsg()
    val url = s"${protocol}://${dom.window.location.host}/ws/edit/abc"
    ws = new dom.WebSocket(url)
    setCallbacks()
  }

  var reconnections = 0

  private def setCallbacks() = {
    ws.onopen = { x: Event =>
      println(s"connected!")
      hideConnectingMsg()
      connected = true
    }
    ws.onmessage = { x: MessageEvent =>
      read[ClientMessage](x.data.toString) match {
        case ClientMessage(Some(sp),_) =>
          println(s"site id = ${sp.siteId}")
          if(reconnections == 0) {
            spreadSheet = Spreadsheet(sp, broadcastCellOperation)
            root.appendChild(spreadSheet.htmlElement)
          } else {
            println(s"resetting")
            spreadSheet.resetSpreadsheetInfo(sp)
            spreadSheet.enableEdition()
          }
        case ClientMessage(_,Some(operation)) =>
          if(operation.from != spreadSheet.siteId) { //@TODO this filtering should be done on the server
            println(s"received operation $operation")
            spreadSheet.receiveRemoteOperation(operation)
          }
      }
    }
    ws.onclose = { x: Event =>
      connected = false
      spreadSheet.disableEdition()
      reconnect()
    }
  }

  private def reconnect() = {
    reconnections += 1
    if(reconnections <= MAX_RECONNECTIONS) {
      connect()
    } else {
      throw new Exception("FAIL HORRIBLY")
    }
  }

  private def broadcastCellOperation(cellOp: SpreadSheetOp): Unit = {
    if(connected) {
      println(s"broadcasting $cellOp")
      ws.send( write(cellOp) )
    } else {
      //pendingOps += cellOp
    }
  }
}
