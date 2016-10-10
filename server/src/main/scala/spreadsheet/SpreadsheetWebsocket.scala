package spreadsheet

import org.http4s._
import org.http4s.server.ServerApp
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.websocket.WebsocketBits._
import org.http4s.dsl._
import org.http4s.server.websocket._

import scalaz.concurrent.Task
import scalaz.concurrent.Strategy
import scalaz.stream.async.unboundedQueue
import scalaz.stream.{DefaultScheduler, Exchange}
import scalaz.stream.time.awakeEvery

import scala.collection.mutable.HashMap

import scalaz.stream.{Exchange, Process, time}
import scalaz.stream.async.topic
import scalaz.syntax.either._
import scalaz.{\/,-\/,\/-}
import scalaz.concurrent.Task

import org.http4s.server.websocket.WS
import org.http4s.websocket.WebsocketBits._

import org.http4s.dsl._

import org.log4s.getLogger
import upickle.legacy._

import woot._

import java.nio.file.{Paths, Files}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, ExecutionContext , Future }
import scala.util.{ Success, Failure }
import spreadsheet.serialization.Writers._
import spreadsheet.serialization.Readers.spreadSheetReader

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
import reactivemongo.bson.document

class SpreadsheetWebsocket {

  val mongoUri = sys.env.get("MONGODB_URI").getOrElse("localhost:27017")

  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = Future.fromTry(parsedUri.map(driver.connection(_)))
  val db: Future[DefaultDB] = connection.flatMap(_.database( sys.env.get("MONGODB_DB").getOrElse("local")))
  val spColl = db.map(_.collection("spreadsheets"))

  var sp: SpreadSheetContent = null

  def saveToMongo(): Task[Unit] = toTask {
    spColl.flatMap(_.update(document("name" -> sp.name), sp, upsert = true)).map(_ => ())
  }

  def toTask[A](future: => Future[A])(implicit ec: ExecutionContext): Task[A] = {
    Task.async { callback =>
      future.onComplete {
        case Success(value) => callback(value.right)
        case Failure(error) => callback(error.left )
      }
    }
  }

  def readFromMongo(name: String): Future[Option[SpreadSheetContent]] = {
    spColl.flatMap(_.find(document("name" -> name)).one[SpreadSheetContent])
  }

  private val ops = topic[SpreadSheetOp]()

  def encodeOp(op: SpreadSheetOp): Text = Text(write(ClientMessage(None, Some(op))))

  def updateSpreadSheet(op: SpreadSheetOp): Task[Throwable \/ SpreadSheetOp] = {
    sp.integrateOperation(op)
    saveToMongo().map { _ =>
      op.right
    }
  }

  def errorHandler(err: Throwable): Task[Unit] = {
    println(s"Failed to consume message: $err")
    Task.fail(err)
  }

  def parse(json: String): Throwable \/ SpreadSheetOp =
    \/.fromTryCatchNonFatal { read[SpreadSheetOp](json) }

  def throwError[A](error: Throwable): Task[Throwable \/ A] = Task.now(error.left)

  def decodeFrame(frame: WebSocketFrame): Task[Throwable \/ SpreadSheetOp] =
    frame match {
      case Text(json, _) =>
        parse(json).fold(throwError, updateSpreadSheet)
      case nonText       =>
        Task.now(new IllegalArgumentException(s"Cannot handle: $nonText").left)
    }

  def getSpreadsheet(name: String): SpreadSheetContent = {
    if(sp != null) {
      sp
    } else {
      val fut = readFromMongo(name).map {
        case Some(_sp) =>
          sp = _sp
          sp
        case None =>
          sp = SpreadSheetContent(SiteId("server"), name, 13, 10)
          sp
      }
      Await.result(fut, 500.millis)
    }
  }

  def safeConsume(consume: SpreadSheetOp => Task[Unit]): WebSocketFrame => Task[Unit] =
         ws => decodeFrame(ws).flatMap(_.fold(errorHandler, consume))

  val route = HttpService {
    case req@ GET -> Root / "edit" / name =>
      val clientId = SiteId.random

      getSpreadsheet(name)

      val clientCopy = sp.withSiteId(clientId)
      val clientCopyJson = write(ClientMessage(Some(clientCopy), None))

      val otherUserOps = ops.subscribe//.filter(_.from != clientId)
      val src = Process.emit( Text(clientCopyJson) ) ++ otherUserOps.map(encodeOp)

      val snk = ops.publish.map(safeConsume).onComplete(cleanup)

      WS(Exchange(src, snk))
  }

  private def cleanup() = {
    println(s"Client disconnect")
    Process.halt
  }

}
