package spreadsheet

import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.server.{Server, ServerApp}
import scalaz.concurrent.Task

class Main {

  def main(args: Array[String]): Unit = {

    val spreadSheetWS = new SpreadsheetWebsocket()

    val port = sys.env.getOrElse("PORT", "5000").toInt

    val staticFilesService = HttpService {
      case req if req.pathInfo == "/"  => req.serve("/index.html")
      case req => req.serve()
    }

    BlazeBuilder
      .bindHttp(port, "0.0.0.0")
      .mountService(staticFilesService, "/")
      .mountService(spreadSheetWS.route, "/ws")
      .start
  }

  implicit class ReqOps(req: Request) {

    def serve(path: String = req.pathInfo) = {
      StaticFile.fromResource(path, Some(req))
        .map(Task.now)
        .getOrElse(NotFound())
    }

  }

}
