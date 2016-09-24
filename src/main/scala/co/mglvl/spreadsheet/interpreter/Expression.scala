package co.mglvl.spreadsheet.interpreter

sealed trait LiteralValue
case class FloatValue(value: Float) extends AnyRef with LiteralValue {
  def +(other: FloatValue)  = FloatValue(value + other.value)
  def *(other: FloatValue)  = FloatValue(value * other.value)
/*
  def <(other: FloatValue) = BooleanValue(value < other.value)
  def <=(other: FloatValue) = BooleanValue(value <= other.value)
  def >(other: FloatValue) = BooleanValue(value > other.value)
  def >=(other: FloatValue) = BooleanValue(value >= other.value)
  def ==(other: FloatValue) = BooleanValue(value == other.value)
*/

  override def toString()   = value.toString
}
/*
case class BooleanValue(value: Boolean) extends AnyRef with LiteralValue {
  override def toString() = value.toString
}
*/
sealed trait Expression[+L<:LiteralValue]
trait CellReference[+L<:LiteralValue] extends Expression[LiteralValue] {
  def id: String
}
//case class IfElse[L<:LiteralValue](condition: BooleanExpression, ifTrue: Expression[L], ifNot: Expression[L]) extends Expression[L]

sealed trait FloatExpression extends Expression[FloatValue]
case class LiteralFloat(value: FloatValue) extends FloatExpression
case class FloatReference(id: String) extends FloatExpression with CellReference[FloatValue]
sealed trait BinaryOp extends FloatExpression {
  def left: FloatExpression
  def right: FloatExpression
}
case class Add(left: FloatExpression, right: FloatExpression) extends BinaryOp
case class Multiply(left: FloatExpression, right: FloatExpression) extends BinaryOp

/*
sealed trait BooleanExpression extends Expression[BooleanValue]
case class BooleanReference(id: Int) extends BooleanExpression
sealed trait Comparison extends BooleanExpression {
  def left: FloatExpression
  def right: FloatExpression
}
case class LessThan(left: FloatExpression, right: FloatExpression) extends Comparison
case class LessThanOrEqual(left: FloatExpression, right: FloatExpression) extends Comparison
case class GreaterThan(left: FloatExpression, right: FloatExpression) extends Comparison
case class GreaterThanOrEqual(left: FloatExpression, right: FloatExpression) extends Comparison
case class Equal(left: FloatExpression, right: FloatExpression) extends Comparison
*/
