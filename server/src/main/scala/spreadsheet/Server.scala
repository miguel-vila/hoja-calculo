package spreadsheet

import org.http4s._, org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.server.{ServerApp, Server => Http4sServer}
import scalaz.concurrent.Task

object Server extends ServerApp {

  override def server(args: List[String]): Task[Http4sServer] = {
    val spreadSheetWS = new SpreadsheetWebsocket()
    val namesService = new NamesService()

    val port = sys.env.getOrElse("PORT", "5000").toInt

    val staticFilesService = HttpService {
      case req if req.pathInfo == "/"  => req.serve("/index.html")
      case req => req.serve()
    }

    BlazeBuilder
      .bindHttp(port, "0.0.0.0")
      .mountService(staticFilesService, "/")
      .mountService(namesService.route, "/")
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
