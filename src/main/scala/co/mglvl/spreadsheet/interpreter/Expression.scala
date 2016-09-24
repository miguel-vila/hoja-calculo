package co.mglvl.spreadsheet.interpreter

sealed trait LiteralValue
case class FloatValue(value: Float) extends AnyRef with LiteralValue {
  def +(other: FloatValue)  = FloatValue(value + other.value)
  def *(other: FloatValue)  = FloatValue(value * other.value)

  override def toString()   = value.toString
}

sealed trait Expression[+L<:LiteralValue]
sealed trait FloatExpression extends Expression[FloatValue]
case class CellReference(id: String) extends FloatExpression
case class LiteralFloat(value: FloatValue) extends FloatExpression
sealed trait BinaryOp extends FloatExpression {
  def left: FloatExpression
  def right: FloatExpression
}
case class Add(left: FloatExpression, right: FloatExpression) extends BinaryOp
case class Multiply(left: FloatExpression, right: FloatExpression) extends BinaryOp
