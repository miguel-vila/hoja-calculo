package spreadsheet

import spreadsheet.ui.Spreadsheet
import spreadsheet.messages.Messages._
import spreadsheet._
import scala.collection.mutable.Queue
import org.scalajs.dom
import upickle.default._
import dom._

import scala.scalajs.js.timers._
import scala.concurrent.duration._

class SpreadsheetConnection(
  root: Element
) {

  private var ws: WebSocket = _
  private var connected = false
  private var spreadSheet: Spreadsheet = null

  private val MAX_RETRIES = 5
  private val initialBackoff = 250.millis
  private var reconnectionBackoff: FiniteDuration = initialBackoff
  private val errorPeriod: FiniteDuration = reconnectionBackoff * Math.pow(2.toDouble, MAX_RETRIES.toDouble - 1).toLong
  private var errorInPeriod = false

  var reconnections = 0

  connect()

  private def connect(): Unit = {
    val protocol = if( window.location.protocol.startsWith("https") ) { "wss" } else { "ws" }
    showConnectingMsg()
    val url = s"${protocol}://${dom.window.location.host}/ws/edit/abc"
    ws = new dom.WebSocket(url)
    setCallbacks()
  }

  private def setCallbacks() = {
    setTimeout(errorPeriod) {
      errorInPeriod = !connected
      if(!errorInPeriod) {
        reconnectionBackoff = initialBackoff
        reconnections = 0
      }
    }
    ws.onopen = { x: Event =>
      println(s"connected!")
      hideConnectingMsg()
      hideDisconnectMsg()
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
      showDisconnectMsg()
      reconnect()
    }
  }

  private def reconnect() = {
    println(s"CONNECTION ERROR")
    connected = false
    hideConnectingMsg()
    if(reconnections > MAX_RETRIES) {
      hideDisconnectMsg()
      showMaxRetriesExceeded()
    } else {
      if( spreadSheet != null ) {
        spreadSheet.disableEdition()
      }
      println(s"retrying in $reconnectionBackoff")
      setTimeout(reconnectionBackoff) {
        reconnectionBackoff = reconnectionBackoff * 2
        if(errorInPeriod) {
          reconnections += 1
        }
        connect()
      }
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
