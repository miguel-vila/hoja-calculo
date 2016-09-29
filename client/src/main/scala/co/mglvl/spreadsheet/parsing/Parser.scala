package co.mglvl.spreadsheet.parsing

import co.mglvl.spreadsheet.interpreter._
import co.spreadsheet.CellId
import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = "(" ~> p <~ ")" | p

  val ws = rep(' ')

  val floatNumber: Parser[FloatValue] = floatingPointNumber.map(s => FloatValue(s.toFloat))

  val reference = '$' ~> regex("[A-Z]+\\d+".r) map { ref: String =>
    val column = (ref.charAt(0) - 'A').toInt
    val row = (ref.charAt(1) - '0').toInt - 1
    new CellReference(CellId(row, column))
  }

  val factor: Parser[Expression] = ws ~> (floatNumber | reference) <~ ws

  val addOp = "+" ^^^ (Add.apply(_,_))

  val multOp = "*" ^^^ (Multiply.apply(_,_))

  val term: Parser[Expression] = chainl1(factor, multOp)

  val expression: Parser[Expression] = chainl1(term, addOp)

  def parse(str: String): ParseResult[Expression] = parseAll(expression, str)

}
