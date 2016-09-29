package co.mglvl.spreadsheet.interpreter

import co.mglvl.spreadsheet.frp.{Cell, Exp}
import co.spreadsheet.CellId

object Interpreter {

  def evaluate(expression: Expression)(env: CellId => Cell): Exp[Float] =
    expression match {
      case FloatValue(n)    => Exp.unit(n)
      case CellReference(id)  => env(id).get().map(_.asInstanceOf[Float])
      case binaryOp: BinaryOp => evaluateBinaryOp(binaryOp)(env)
    }

  def evaluateBinaryOp(binaryOp: BinaryOp)(env: CellId => Cell): Exp[Float] = {
    def operate(f: (Float, Float) => Float): Exp[Float] = {
      Exp.map2(evaluate(binaryOp.left)(env), evaluate(binaryOp.right)(env)) (f)
    }
    binaryOp match {
      case _: Add       => operate( _ + _ )
      case _: Multiply  => operate( _ * _ )
    }
  }

}
