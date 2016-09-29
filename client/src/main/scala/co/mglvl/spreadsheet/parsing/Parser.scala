package co.mglvl.spreadsheet.parsing

import co.mglvl.spreadsheet.interpreter._
import co.spreadsheet.CellId
import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = "(" ~> p <~ ")" | p

  val ws = rep(' ')

  val floatNumber: Parser[FloatValue] = floatingPointNumber.map(s => FloatValue(s.toFloat))

  val reference = ( '$' ~> regex("[A-Z]".r) ~ regex("\\d".r) ) map { case colstr ~ rowstr =>
    val column = (colstr.charAt(0) - 'A').toInt
    val row = (rowstr.charAt(0) - '0').toInt - 1
    new CellReference(CellId(row, column))
  }

  val factor: Parser[Expression] = ws ~> (floatNumber | reference) <~ ws

  val addOp = "+" ^^^ (Add.apply(_,_))

  val multOp = "*" ^^^ (Multiply.apply(_,_))

  val term: Parser[Expression] = chainl1(factor, multOp)

  val string: Parser[StringValue] = (regex("[A-Za-z]*".r)).map{ case s => println(s); StringValue( s) }

  val expression: Parser[Expression] =
    ( '=' ~> chainl1(term, addOp)) |
      floatNumber |
      string

  def parse(str: String): ParseResult[Expression] = parseAll(expression, str)

}
