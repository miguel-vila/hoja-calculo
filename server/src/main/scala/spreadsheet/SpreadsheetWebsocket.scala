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
import scalaz.stream.{Process, Sink}
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
import upickle.default._

import woot._

import java.io._
import scala.io._
import java.nio.file.{Paths, Files}

class SpreadsheetWebsocket {

  //val spreadSheets: HashMap[String, SpreadSheetContent] = HashMap.empty

  def readFromFile(name: String): SpreadSheetContent = {
    val source = Source.fromFile( s"$name.json" )
    val lines = try source.mkString finally source.close()
    read[SpreadSheetContent](lines)
  }

  private val ops = topic[SpreadSheetOp]()

  def encodeOp(op: SpreadSheetOp): Text = Text(write(ClientMessage(None, Some(op))))

  def dumpToFile(sp: SpreadSheetContent): Unit = {
    val file = new File(s"${sp.name}.json")
    val pw = new PrintWriter(file)
    pw.write(write(sp))
    pw.close()
  }

  def updateSpreadSheet(sp: SpreadSheetContent)(op: SpreadSheetOp): SpreadSheetOp = {
    sp.integrateOperation(op)
    dumpToFile(sp)
    op
  }

  def errorHandler(err: Throwable): Task[Unit] = {
    println(s"Failed to consume message: $err")
    Task.fail(err)
  }

  def parse(json: String): Throwable \/ SpreadSheetOp =
    \/.fromTryCatchNonFatal { read[SpreadSheetOp](json) }

  def decodeFrame(frame: WebSocketFrame, sp: SpreadSheetContent): Throwable \/ SpreadSheetOp =
    frame match {
      case Text(json, _) => parse(json).map(updateSpreadSheet(sp))
      case nonText       => new IllegalArgumentException(s"Cannot handle: $nonText").left
    }

  def safeConsume(sp: SpreadSheetContent)(consume: SpreadSheetOp => Task[Unit]): WebSocketFrame => Task[Unit] =
         ws => decodeFrame(ws, sp).fold(errorHandler, consume)

  val route = HttpService {
    case req@ GET -> Root / "edit" / name =>
      val clientId = SiteId.random

      val sp =
        if(Files.exists(Paths.get(s"$name.json"))) {
          readFromFile(name)
        } else {
          val sp = SpreadSheetContent(SiteId("server"), name, 13, 10)
          dumpToFile(sp)
          sp
        }

      val clientCopy =
        sp.withSiteId(clientId)

      val otherUserOps = ops.subscribe//.filter(_.from != clientId)
      //println(s"about to serialize $clientCopy")
      val serializedCopy = write(ClientMessage(Some(clientCopy), None))
      //println(s"serialized $serializedCopy")
      val src = Process.emit(Text( serializedCopy )) ++ otherUserOps.map(encodeOp)
      val snk = ops.publish.map(safeConsume(sp)).onComplete(cleanup)

      WS(Exchange(src, snk))
  }

  private def cleanup() = {
    println(s"Client disconnect")
    Process.halt
  }

}
