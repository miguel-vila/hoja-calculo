package co.mglvl.spreadsheet.interpreter

sealed trait LiteralValue
case class FloatValue(value: Float) extends LiteralValue {
  def +(other: FloatValue) = FloatValue(value + other.value)
  def *(other: FloatValue) = FloatValue(value * other.value)
  def <=(other: FloatValue) = BooleanValue(value <= other.value)
  override def toString() = value.toString
}
case class BooleanValue(value: Boolean) extends LiteralValue

sealed trait Expression[+L<:LiteralValue]
trait CellReference[+L<:LiteralValue] extends Expression[LiteralValue] {
  def id: Int
}

sealed trait FloatExpression extends Expression[FloatValue]
case class LiteralFloat(value: FloatValue) extends FloatExpression
case class FloatReference(id: Int) extends FloatExpression with CellReference[FloatValue]
sealed trait BinaryOp extends FloatExpression
case class Add(left: FloatExpression, right: FloatExpression) extends BinaryOp
case class Multiply(left: FloatExpression, right: FloatExpression) extends BinaryOp
case class IfElse(condition: BooleanExpression, ifTrue: FloatExpression, ifNot: FloatExpression) extends FloatExpression

sealed trait BooleanExpression extends Expression[BooleanValue]
case class LessThanOrEqual(left: FloatExpression, right: FloatExpression) extends BooleanExpression