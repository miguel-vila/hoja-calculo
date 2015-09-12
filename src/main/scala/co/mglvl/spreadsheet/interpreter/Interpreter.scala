package co.mglvl.spreadsheet.interpreter

import co.mglvl.spreadsheet._
import co.mglvl.spreadsheet.frp.{Cell, Exp}

object Interpreter {

  def getReferences(exp: Expression): Set[Int] = {
    exp match {
      case _: Literal => Set.empty
      case CellReference(id) => Set(id)
      case Add(left,right) => getReferences(left) union getReferences(right)
      case Multiply(left,right) => getReferences(left) union getReferences(right)
    }
  }

  def evaluate(expression: Expression)(env: Int => Cell[Float]): Exp[Float] =
    expression match {
      case Literal        (n)           => Exp.unit( n )
      case CellReference  (id)          => env(id).get()
      case Add            (left,right)  => Exp.map2( evaluate(left)(env) , evaluate(right)(env) )(_+_)
      case Multiply       (left,right)  => Exp.map2( evaluate(left)(env) , evaluate(right)(env) )(_*_)
    }

}
