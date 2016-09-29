package spreadsheet.interpreter

import spreadsheet.frp.{Cell, Exp}
import spreadsheet.CellId

object Interpreter {

  def evaluate(expression: Expression)(env: CellId => Cell): Exp[Value] =
    expression match {
      case value: Value       => Exp.unit(value)
      case CellReference(id)  => env(id).get().map(_.asInstanceOf[FloatValue])
      case binaryOp: BinaryOp => evaluateBinaryOp(binaryOp)(env)
    }

  def evaluateBinaryOp(binaryOp: BinaryOp)(env: CellId => Cell): Exp[Value] = {
    def operate(f: (FloatValue, FloatValue) => FloatValue): Exp[FloatValue] = {
      Exp.map2(
        evaluate(binaryOp.left)(env).map(_.asInstanceOf[FloatValue]),
        evaluate(binaryOp.right)(env).map(_.asInstanceOf[FloatValue])
      ) (f)
    }
    binaryOp match {
      case _: Add       => operate( _ + _ )
      case _: Multiply  => operate( _ * _ )
    }
  }

}
