package spreadsheet.interpreter

import spreadsheet.frp.{Cell, Exp}
import spreadsheet.CellId

object Interpreter {

  def evaluate(expression: Expression)(env: CellId => Cell): Exp[Value] =
    expression match {
      case value: Value          => Exp.unit(value)
      case CellReference(id)     => env(id).get()
      case binaryOp: BinaryOp    => evaluateBinaryOp(binaryOp)(env)
      case SumInRange(CellsRange(start,end)) =>
        val exps = for {
          row <- start.row to end.row
          column <- start.column to end.column
        } yield {
          env(CellId(row,column)).get()
        }
        exps.foldLeft( Exp.unit(FloatValue(0.0f)) ) { (accExp, exp) =>
          for {
            acc <- accExp
            value <- exp
          } yield acc + value.asInstanceOf[FloatValue]
        }
    }

  def evaluateBinaryOp(binaryOp: BinaryOp)(env: CellId => Cell): Exp[Value] = {
    def operate(f: (FloatValue, FloatValue) => FloatValue): Exp[FloatValue] = {
      val leftValue = evaluate(binaryOp.left)(env).map(castToFloatValue)
      val rightValue = evaluate(binaryOp.right)(env).map(castToFloatValue)
      Exp.map2(
        leftValue,
        rightValue
      ) (f)
    }
    binaryOp match {
      case _: Add       => operate( _ + _ )
      case _: Multiply  => operate( _ * _ )
    }
  }

  def castToFloatValue(value: Value): FloatValue = value match {
    case FloatValue(n)   => FloatValue(n)
    case StringValue("") => FloatValue(0)
    case _               =>
      throw new Exception(s"Wrong type for $value, expected a numerical value")
  }

}
