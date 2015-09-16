package co.mglvl.spreadsheet.parsing

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.interpreter._

import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = {
    val parenthesised = ("(" ~ p ~ ")") map { case ~(~(_,p),_) => p }
    parenthesised | p
  }

  val floatNumber: Parser[LiteralFloat] = floatingPointNumber.map(s => LiteralFloat(FloatValue(s.toFloat)))

  val cellReference: Parser[FloatReference] = ("$" ~ regex("\\d+".r)).map { case ~(_,ref) => FloatReference(ref.toInt) }

  val factor: Parser[FloatExpression] = floatNumber | cellReference

  val term: Parser[FloatExpression] = ( factor ~ rep("*" ~ factor) ) map { case ~(e1,es) =>
    accumulateTerm(e1,es)
  }

  val expression: Parser[FloatExpression] = ( term ~ rep( ("+" ~ term) )).map { case ~(e1,es) =>
    accumulateExp(e1,es)
  }

  def accumulateExp(init: FloatExpression, rest: List[String ~ FloatExpression]): FloatExpression = {
    rest.foldLeft(init) { case (expAcc,~(op, exp)) => Add(expAcc, exp) }
  }

  def accumulateTerm(init: FloatExpression, rest: List[String ~ FloatExpression]): FloatExpression = {
    rest.foldLeft(init) { case (expAcc,~(op, exp)) => Multiply(expAcc,exp) }
  }

  def parse(str: String): ParseResult[Expression[_]] = parseAll(expression, str)

}
