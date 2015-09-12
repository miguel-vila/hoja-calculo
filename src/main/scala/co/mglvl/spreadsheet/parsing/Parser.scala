package co.mglvl.spreadsheet.parsing

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.interpreter._

import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = {
    val parenthesised = ("(" ~ p ~ ")") map { case ~(~(_,p),_) => p }
    parenthesised | p
  }

  val floatNumber: Parser[Literal] = floatingPointNumber.map(s => Literal(s.toFloat))

  val cellReference: Parser[CellReference] = ("$" ~ regex("\\d+".r)).map { case ~(_,ref) => CellReference(ref.toInt) }

  val factor: Parser[Expression] = floatNumber | cellReference

  val term: Parser[Expression] = ( factor ~ rep("*" ~ factor) ) map { case ~(e1,es) =>
    accumulateTerm(e1,es)
  }

  val expression: Parser[Expression] = ( term ~ rep( ("+" ~ term) )).map { case ~(e1,es) =>
    accumulateExp(e1,es)
  }

  def accumulateExp(init: Expression, rest: List[String ~ Expression]): Expression = {
    rest.foldLeft(init)( { case (expAcc,~(op, exp)) =>
      op match {
        case "+" => Add(expAcc, exp)
      }
    })
  }

  def accumulateTerm(init: Expression, rest: List[String ~ Expression]): Expression = {
    rest.foldLeft(init)( { case (expAcc,~(op, exp)) =>
      op match {
        case "*" => Multiply(expAcc,exp)
      }
    })
  }

  def parse(str: String): ParseResult[Expression] = parseAll(expression, str)

}
