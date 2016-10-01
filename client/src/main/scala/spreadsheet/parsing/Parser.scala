package spreadsheet.parsing

import spreadsheet.interpreter._
import spreadsheet.CellId
import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = "(" ~> p <~ ")" | p

  val ws = rep(' ')

  val floatNumber: Parser[FloatValue] = floatingPointNumber.map(s => FloatValue(s.toFloat))

  val cellId = ( '$' ~> regex("[A-Z]".r) ~ regex("\\d+".r) ) map { case colstr ~ rowstr =>
    val column = (colstr.charAt(0) - 'A').toInt
    val row = Integer.parseInt(rowstr) - 1
    CellId(row, column)
  }

  val reference = cellId map (CellReference.apply)

  val range = ( (cellId <~ ':') ~ cellId) map { case ref1 ~ ref2 =>
    CellsRange(ref1,ref2)
  }

  val sumRange = ("sum" ~> '(' ~> range <~ ')') map (SumInRange.apply)

  val factor: Parser[Expression] = ws ~> (floatNumber | reference | sumRange) <~ ws

  val addOp = "+" ^^^ (Add.apply(_,_))

  val multOp = "*" ^^^ (Multiply.apply(_,_))

  val term: Parser[Expression] = chainl1(factor, multOp)

  val string: Parser[StringValue] = (regex("[A-Za-z]*".r)).map{ case s => StringValue( s) }

  val expression: Parser[Expression] =
    ( '=' ~> chainl1(term, addOp)) |
      floatNumber |
      string

  def parse(str: String): ParseResult[Expression] = parseAll(expression, str)

}
