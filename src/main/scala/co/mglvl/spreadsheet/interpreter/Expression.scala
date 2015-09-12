package co.mglvl.spreadsheet.interpreter

sealed trait Expression
case class Literal(value: Float) extends Expression
case class CellReference(id: Int) extends Expression
sealed trait BinaryOp extends Expression
case class Add(left: Expression, right: Expression) extends BinaryOp
case class Multiply(left: Expression, right: Expression) extends BinaryOp
