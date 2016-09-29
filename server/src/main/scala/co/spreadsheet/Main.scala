package co.spreadsheet

import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.server.{Server, ServerApp}
import scalaz.concurrent.Task

object Main extends ServerApp {

  val spreadSheetWS = new SpreadsheetWebsocket()

  override def server(args: List[String]): Task[Server] = {
    BlazeBuilder
      .bindHttp(80, "localhost")
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

  val staticFilesService = HttpService {
    case req if req.pathInfo == "/"  => req.serve("/index.html")
    case req => req.serve()
  }


}
