package spreadsheet.serialization

import reactivemongo.bson._
import woot._
import spreadsheet.SpreadSheetContent

object Writers {
  implicit val charWriter = new BSONWriter[Char, BSONValue] {
    override def write(c: Char) = BSONString(c.toString)
  }
  implicit val clockValueWriter = Macros.writer[ClockValue]
  implicit val siteIdWriter = Macros.writer[SiteId]
  implicit val charIdWriter = Macros.writer[CharId]

  implicit val idWriter = new BSONWriter[Id, BSONValue] {
    override def write(id: Id) = id match {
      case Beginning      => BSONString("Beginning")
      case Ending         => BSONString("Ending")
      case charId: CharId => charIdWriter.write(charId)
    }
  }

  implicit val insertOpWriter: BSONDocumentWriter[InsertOp] = Macros.writer[InsertOp]
  implicit val deleteOpWriter: BSONDocumentWriter[DeleteOp] = Macros.writer[DeleteOp]

  implicit val operationWriter = new BSONWriter[Operation, BSONValue] {
    def write(op: Operation) = op match {
      case insertOp: InsertOp => insertOpWriter.write(insertOp).add(BSONDocument(List("type" -> BSONString("insert"))))
      case deleteOp: DeleteOp => deleteOpWriter.write(deleteOp).add(BSONDocument(List("type" -> BSONString("delete"))))
    }
  }

  implicit val wCharWriter = Macros.writer[WChar]
  implicit val wStringWriter = Macros.writer[WString]
  implicit val spreadSheetWriter: BSONDocumentWriter[SpreadSheetContent] = Macros.writer[SpreadSheetContent]
}
