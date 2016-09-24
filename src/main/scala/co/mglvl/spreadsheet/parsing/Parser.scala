package co.mglvl.spreadsheet.parsing

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.interpreter._

import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = "(" ~> p <~ ")" | p

  val ws = rep(" ")

  def withOptionalWhitespace[A](p: Parser[A]): Parser[A] = (ws ~> p <~ ws)

  val floatNumber: Parser[LiteralFloat] = withOptionalWhitespace( floatingPointNumber ).map(s => LiteralFloat(FloatValue(s.toFloat)))

  val reference = withOptionalWhitespace("$" ~> regex("[A-Z]+\\d+".r)) map { ref => new CellReference(ref) }

  val factor: Parser[FloatExpression] = floatNumber | reference

  val addOp = "+" ^^^ (Add.apply(_,_))

  val multOp = "*" ^^^ (Multiply.apply(_,_))

  val term: Parser[FloatExpression] = chainl1(factor, multOp)

  val floatExpression: Parser[FloatExpression] = chainl1(term, addOp)

  val expression: Parser[Expression[_<:LiteralValue]] = floatExpression

  def parse(str: String): ParseResult[Expression[_]] = parseAll(expression, str)

}
