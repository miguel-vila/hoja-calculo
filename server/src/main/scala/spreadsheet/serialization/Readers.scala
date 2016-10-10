package spreadsheet.serialization

import reactivemongo.bson._
import woot._
import spreadsheet.SpreadSheetContent

object Readers {

  implicit val charReader = new BSONReader[BSONString, Char] {
    override def read(bstr: BSONString) = {
      val BSONString(value) =  bstr
      value(0)
    }
  }
  implicit val clockValueReader = Macros.reader[ClockValue]
  implicit val siteIdReader = Macros.reader[SiteId]
  implicit val charIdReader = Macros.reader[CharId]

  implicit val idReader = new BSONReader[BSONValue, Id] {
    def read(bsonValue: BSONValue): Id = bsonValue match {
      case BSONString("Beginning") => Beginning
      case BSONString("Ending") => Ending
      case bsonDoc: BSONDocument => charIdReader.read(bsonDoc)
    }
  }
  implicit val wCharReader = Macros.reader[WChar]

  implicit val insertOpReader: BSONDocumentReader[InsertOp] = Macros.reader[InsertOp]
  implicit val deleteOpReader: BSONDocumentReader[DeleteOp] = Macros.reader[DeleteOp]

  implicit val operationReader: BSONDocumentReader[Operation] = new BSONDocumentReader[Operation] {
    def read(bsonDoc: BSONDocument) = (bsonDoc.getAs[String]("type")) match {
      case Some("insert") => insertOpReader.read(bsonDoc)
      case Some("delete") => deleteOpReader.read(bsonDoc)
      case _ => throw new Exception("FUUUUUCK")
    }
  }

  implicit val wStringReader = Macros.reader[WString]
  implicit val spreadSheetReader: BSONDocumentReader[SpreadSheetContent] = Macros.reader[SpreadSheetContent]

}
