package spreadsheet

import spreadsheet.ui.Spreadsheet
import spreadsheet.messages.Messages._
import spreadsheet._
import scala.collection.mutable.Queue
import org.scalajs.dom
import upickle.legacy._
import dom._

import scala.scalajs.js.timers._
import scala.concurrent.duration._

class SpreadsheetConnection(
  root: Element
) {

  private var ws: WebSocket = _
  private var connected = false
  private var spreadSheet: Spreadsheet = null
  private var pendingOps = Queue.empty[SpreadSheetOp]

  private val MAX_RETRIES = 5
  private val initialBackoff = 250.millis
  private var reconnectionBackoff = initialBackoff
  private var reconnections = 0

  connect()

  private def connect(): Unit = {
    val protocol = if( window.location.protocol.startsWith("https") ) { "wss" } else { "ws" }
    showConnectingMsg()
    val url = s"${protocol}://${dom.window.location.host}/ws/edit/abc"
    ws = new dom.WebSocket(url)
    setCallbacks()
  }

  private def setCallbacks() = {
    ws.onopen = { x: Event =>
      println(s"connected!")
      hideConnectingMsg()
      hideDisconnectMsg()
      reconnections = 0
      reconnectionBackoff = initialBackoff
      connected = true
    }
    ws.onmessage = { x: MessageEvent =>
      read[ClientMessage](x.data.toString) match {
        case ClientMessage(Some(sp),_) =>
          println(s"site id = ${sp.siteId}")
          if(spreadSheet == null) {
            spreadSheet = Spreadsheet(sp, broadcastCellOperation)
            root.appendChild(spreadSheet.htmlElement)
          } else {
            println(s"resetting")
            spreadSheet.resetSpreadsheetInfo(sp)
            while(!pendingOps.isEmpty) {
              ws.send( write( pendingOps.dequeue() ) )
            }
          }
        case ClientMessage(_,Some(operation)) =>
          if(operation.from != spreadSheet.siteId) { //@TODO this filtering should be done on the server
            println(s"received operation $operation")
            spreadSheet.receiveRemoteOperation(operation)
          }
      }
    }
    ws.onclose = { x: Event =>
      reconnect()
    }
  }

  private def reconnect() = {
    println(s"CONNECTION ERROR")
    connected = false
    if(reconnections > MAX_RETRIES) {
      spreadSheet.disableEdition()
      showMaxRetriesExceeded()
    } else {
      println(s"retrying in $reconnectionBackoff")
      setTimeout(reconnectionBackoff) {
        reconnectionBackoff = reconnectionBackoff * 2
        reconnections += 1
        connect()
      }
    }
  }

  private def broadcastCellOperation(cellOp: SpreadSheetOp): Unit = {
    if(connected) {
      println(s"broadcasting $cellOp")
      ws.send( write(cellOp) )
    } else {
      println(s"stashing $cellOp")
      pendingOps += cellOp
    }
  }
}
