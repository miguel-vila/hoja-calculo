package spreadsheet.parsing

import spreadsheet.interpreter._
import spreadsheet.CellId
import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = "(" ~> p <~ ")" | p

  val ws = rep(' ')

  val number: Parser[NumberValue] = floatingPointNumber.map(s => NumberValue(s.toDouble))

  val cellId = ( '$'.? ~> regex("[A-Z]".r) ~ regex("\\d+".r) ) map { case colstr ~ rowstr =>
    val column = (colstr.charAt(0) - 'A').toInt
    val row = Integer.parseInt(rowstr) - 1
    CellId(row, column)
  }

  val reference = cellId map (CellReference.apply)

  val range = ( (cellId <~ ':') ~ cellId) map { case ref1 ~ ref2 =>
    CellsRange(ref1,ref2)
  }

  val sumRange = ("sum" ~> '(' ~> range <~ ')') map (SumInRange.apply)

  def factor(withReferences: Boolean): Parser[Expression] = {
    val fact = if(withReferences) {
      number | reference | sumRange
    } else {
      number
    }
    ws ~> fact <~ ws
  }

  val addOp = "+" ^^^ (Add.apply(_,_))

  val multOp = "*" ^^^ (Multiply.apply(_,_))

  def term(withReferences: Boolean): Parser[Expression] = chainl1(factor(withReferences), multOp)

  val string: Parser[StringValue] = (regex("[A-Za-z]*".r)).map{ case s => StringValue( s) }

  def arithmeticExpression(withReferences: Boolean) = chainl1(term(withReferences), addOp)

  val expression: Parser[Expression] =
    ( '=' ~> arithmeticExpression(withReferences = true) ) |
      arithmeticExpression(withReferences = false) |
      number |
      string

  def parse(str: String): ParseResult[Expression] = parseAll(expression, str)

}
