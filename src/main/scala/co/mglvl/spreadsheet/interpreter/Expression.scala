package co.mglvl.spreadsheet.interpreter

sealed trait Expression
case class FloatValue(value: Float) extends AnyRef with Expression {
  def +(other: FloatValue)  = FloatValue(value + other.value)
  def *(other: FloatValue)  = FloatValue(value * other.value)

  override def toString()   = value.toString
}
case class CellReference(id: String) extends Expression
sealed trait BinaryOp extends Expression {
  def left: Expression
  def right: Expression
}
case class Add(left: Expression, right: Expression) extends BinaryOp
case class Multiply(left: Expression, right: Expression) extends BinaryOp
