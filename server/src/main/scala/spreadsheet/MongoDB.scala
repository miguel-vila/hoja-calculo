package spreadsheet

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
import reactivemongo.bson.{ document, BSONBoolean, BSONDocument }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MongoDB {

  val mongoUri = sys.env.get("MONGODB_URI").getOrElse("localhost:27017")

  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = Future.fromTry(parsedUri.map(driver.connection(_)))
  val db: Future[DefaultDB] = connection.flatMap(_.database( sys.env.get("MONGODB_DB").getOrElse("local")))
  val spColl = db.map(_.collection("spreadsheets"))

}
