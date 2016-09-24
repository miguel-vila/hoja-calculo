package co.mglvl.spreadsheet.interpreter

import co.mglvl.spreadsheet.frp.{Cell, Exp}

object Interpreter {

  def evaluate(expression: Expression[_])(env: String => Cell[LiteralValue]): Exp[LiteralValue] =
    expression match {
      case cellRef: CellReference => env(cellRef.id).get()
      case floatExpression: FloatExpression => evaluateFloatExpression(floatExpression)(env)
    }

  def evaluateFloatExpression(expression: FloatExpression)(env: String => Cell[LiteralValue]): Exp[FloatValue] = {
    expression match {
      case LiteralFloat(n)    => Exp.unit(n)
      case CellReference(id)  => env(id).get().map(_.asInstanceOf[FloatValue])
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

}
