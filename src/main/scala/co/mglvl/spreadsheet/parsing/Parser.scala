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

  val addOp = "+" ^^^ (Add.apply(_,_))

  val multOp = "*" ^^^ (Multiply.apply(_,_))

  val term: Parser[FloatExpression] = chainl1(factor, multOp)

  val floatExpression: Parser[FloatExpression] = chainl1(term, addOp)

  /*
  def comparisonParser[C<:Comparison](symbol: String, constructor: (FloatExpression, FloatExpression) => C): Parser[C] =
    (floatExpression ~ symbol ~ floatExpression) map {
      case e1 ~ _ ~ e2 => constructor(e1,e2)
    }
  */

  /*
  val lessThan        : Parser[LessThan]            = comparisonParser("<"  , LessThan.apply          )
  val lessThanOrEq    : Parser[LessThanOrEqual]     = comparisonParser("<=" , LessThanOrEqual.apply   )
  val greaterThan     : Parser[GreaterThan]         = comparisonParser(">"  , GreaterThan.apply       )
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
  */

  val expression: Parser[Expression[_<:LiteralValue]] = floatExpression

  def parse(str: String): ParseResult[Expression[_]] = parseAll(expression, str)

}
