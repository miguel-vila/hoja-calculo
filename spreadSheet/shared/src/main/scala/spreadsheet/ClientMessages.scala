package spreadsheet

case class ClientMessage(
  cells: Option[SpreadSheetContent],
  operation: Option[SpreadSheetOp]
)
