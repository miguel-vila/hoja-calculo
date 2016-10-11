package spreadsheet

import org.http4s._
import org.http4s.server.ServerApp
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.dsl._

import upickle.legacy._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, ExecutionContext , Future }

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
import reactivemongo.bson.{ document, BSONBoolean, BSONDocument }

class NamesService {

  val spColl = MongoDB.spColl

  val route = HttpService {
    case req @ GET -> Root / "spreadsheet-names" =>
      Ok(getSpreadsheetNames().map( names =>  write(names) ))
  }

  def getSpreadsheetNames(): Future[List[String]] = {
    for {
      coll <- spColl
      results <- coll.find(document(),
                           document("name"-> BSONBoolean(true))
      ).cursor[BSONDocument]().collect[List]()
    } yield results.map(_.getAs[String]("name").get)
  }
}
