package co.mglvl.spreadsheet.parsing

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.interpreter._

import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers with RegexParsers {

  def optionallyParenthesised[A](p: Parser[A]): Parser[A] = "(" ~> p <~ ")" | p

  val ws = rep(" ")

  def withOptionalWhitespace[A](p: Parser[A]): Parser[A] = (ws ~> p <~ ws)

  val floatNumber: Parser[LiteralFloat] = withOptionalWhitespace( floatingPointNumber ).map(s => LiteralFloat(FloatValue(s.toFloat)))

  val reference = withOptionalWhitespace("$" ~> regex("\\d+".r)) map { _.toInt }

  val floatReference: Parser[FloatReference] = reference map { ref => FloatReference(ref) }

  val factor: Parser[FloatExpression] = floatNumber | floatReference

  val term: Parser[FloatExpression] = ( factor ~ rep("*" ~> factor) ) map { case e1 ~ es =>
    accumulateTerm(e1,es)
  }

  val floatExpression: Parser[FloatExpression] = ( term ~ rep( ("+" ~> term) )).map { case e1 ~ es =>
    accumulateExp(e1,es)
  }

  def accumulateExp(init: FloatExpression, rest: List[FloatExpression]): FloatExpression = {
    rest.foldLeft(init) { case (expAcc, exp) => Add(expAcc, exp) }
  }

  def accumulateTerm(init: FloatExpression, rest: List[FloatExpression]): FloatExpression = {
    rest.foldLeft(init) { case (expAcc, exp) => Multiply(expAcc,exp) }
  }

  def comparisonParser[C<:Comparison](symbol: String, constructor: (FloatExpression, FloatExpression) => C): Parser[C] =
    (floatExpression ~ symbol ~ floatExpression) map {
      case e1 ~ _ ~ e2 => constructor(e1,e2)
    }

//  val lessThan        : Parser[LessThan]            = comparisonParser("<"  , LessThan.apply          )
  val lessThanOrEq    : Parser[LessThanOrEqual]     = comparisonParser("<=" , LessThanOrEqual.apply   )
//  val greaterThan     : Parser[GreaterThan]         = comparisonParser(">"  , GreaterThan.apply       )
  val greaterThanOrEq : Parser[GreaterThanOrEqual]  = comparisonParser(">=" , GreaterThanOrEqual.apply)
  val eq              : Parser[Equal]               = comparisonParser("==" , Equal.apply             )

  val comparison      : Parser[Comparison]          = lessThanOrEq | greaterThanOrEq | eq

  val booleanReference: Parser[BooleanReference] = reference map { ref => BooleanReference(ref) }

  val booleanExpression: Parser[BooleanExpression] = comparison | booleanReference

  val ifElse: Parser[IfElse[_<:LiteralValue]] = {
    val condition = "if" ~> "(" ~> booleanExpression <~ ")"
    val exp = "{" ~> expression <~ "}"
    ( (condition ~ exp) ~ ("else" ~> exp) ) map { case (condition ~ ifTrue ~ ifFalse) =>
      IfElse(condition, ifTrue, ifFalse)
    }
  }

  val expression: Parser[Expression[_<:LiteralValue]] = booleanExpression | floatExpression | ifElse

  def parse(str: String): ParseResult[Expression[_]] = parseAll(expression, str)

}
