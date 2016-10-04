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
        exps.foldLeft( Exp.unit(NumberValue(0.0)) ) { (accExp, exp) =>
          for {
            acc <- accExp
            value <- exp
          } yield acc +  castToNumberValue( value )
        }
    }

  def evaluateBinaryOp(binaryOp: BinaryOp)(env: CellId => Cell): Exp[Value] = {
    def operate(f: (NumberValue, NumberValue) => NumberValue): Exp[NumberValue] = {
      val leftValue = evaluate(binaryOp.left)(env).map(castToNumberValue)
      val rightValue = evaluate(binaryOp.right)(env).map(castToNumberValue)
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

  def castToNumberValue(value: Value): NumberValue = value match {
    case NumberValue(n)  => NumberValue(n)
    case StringValue("") => NumberValue(0)
    case _               =>
      throw new Exception(s"Wrong type for $value, expected a numerical value")
  }

}
