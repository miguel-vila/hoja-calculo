package spreadsheet

import org.http4s._
import org.http4s.websocket.WebsocketBits._
import org.http4s.dsl._
import org.http4s.server.websocket._
import org.http4s.server.websocket.WS

class SpreadsheetWebsocket {

  private var spreadsheets = Map.empty[String, SpreadsheetState]

  val route = HttpService {
    case req @ GET -> Root / "edit" / name =>
      val sp = spreadsheets.get(name).getOrElse {
        println(s"CREATING NEW SP: $name")
        val newSp = new SpreadsheetState(name)
        spreadsheets = spreadsheets.updated(name, newSp)
        newSp
      }
      sp.createWebsocketConnection()
  }

}
