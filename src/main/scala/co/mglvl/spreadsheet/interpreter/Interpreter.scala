package co.mglvl.spreadsheet.interpreter

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.frp.{Cell, Exp}

object Interpreter {

  def evaluate(expression: Expression[_])(env: String => Cell[LiteralValue]): Exp[LiteralValue] =
    expression match {
      case cellRef: CellReference[_] => env(cellRef.id).get()
      case floatExpression: FloatExpression => evaluateFloatExpression(floatExpression)(env)
        /*
      case booleanExpression: BooleanExpression => evaluateBooleanExpression(booleanExpression)(env)
      case IfElse(condition,ifTrue,ifNot) =>
        evaluateBooleanExpression(condition)(env).flatMap { condition =>
          if(condition.value) {
            evaluate(ifTrue)(env)
          } else {
            evaluate(ifNot)(env)
          }
        }
        */
    }

  def evaluateFloatExpression(expression: FloatExpression)(env: String => Cell[LiteralValue]): Exp[FloatValue] = {
    expression match {
      case LiteralFloat(n)    => Exp.unit(n)
      case FloatReference(id) => env(id).get().map(_.asInstanceOf[FloatValue])
      case binaryOp: BinaryOp => evaluateBinaryOp(binaryOp)(env)
    }
  }

  def evaluateBinaryOp(binaryOp: BinaryOp)(env: String => Cell[LiteralValue]): Exp[FloatValue] = {
    def operate(f: (FloatValue, FloatValue) => FloatValue): Exp[FloatValue] = {
      Exp.map2(evaluateFloatExpression(binaryOp.left)(env), evaluateFloatExpression(binaryOp.right)(env)) (f)
    }
    binaryOp match {
      case _: Add       => operate( _ + _ )
      case _: Multiply  => operate( _ * _ )
    }
  }
  /*

  def evaluateBooleanExpression(expression: BooleanExpression)(env: String => Cell[LiteralValue]): Exp[BooleanValue] =
    expression match {
      case BooleanReference(id)   => env(id).get().map(_.asInstanceOf[BooleanValue])
      case comparison: Comparison => evaluateComparison(comparison)(env)
    }

  def evaluateComparison(comparison: Comparison)(env: String => Cell[LiteralValue]): Exp[BooleanValue] = {
    def compare(f: (FloatValue, FloatValue) => BooleanValue) = {
      Exp.map2(evaluateFloatExpression(comparison.left)(env), evaluateFloatExpression(comparison.right)(env))(f)
    }
    comparison match {
      case LessThan           (left, right) => compare(_ < _)
      case LessThanOrEqual    (left, right) => compare(_ <= _)
      case GreaterThan        (left, right) => compare(_ > _)
      case GreaterThanOrEqual (left, right) => compare(_ >= _)
      case Equal              (left, right) => compare(_ == _)
    }
  }
  */


}
