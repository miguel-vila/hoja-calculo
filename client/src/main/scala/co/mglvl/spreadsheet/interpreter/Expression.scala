package co.mglvl.spreadsheet.interpreter

import co.spreadsheet.CellId

sealed trait Expression
case class FloatValue(value: Float) extends AnyRef with Expression {
  def +(other: FloatValue)  = FloatValue(value + other.value)
  def *(other: FloatValue)  = FloatValue(value * other.value)

  override def toString()   = value.toString
}
case class CellReference(cellId: CellId) extends AnyRef with Expression
sealed trait BinaryOp extends Expression {
  def left: Expression
  def right: Expression
}
case class Add(left: Expression, right: Expression) extends BinaryOp
case class Multiply(left: Expression, right: Expression) extends BinaryOp
