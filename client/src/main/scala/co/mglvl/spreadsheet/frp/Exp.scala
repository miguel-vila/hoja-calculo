package co.mglvl.spreadsheet.frp

case class Exp[+A](thunk: () => (A, Set[Cell])) extends AnyRef {

  def apply(): (A, Set[Cell]) = thunk()

  def map[B](f: A => B): Exp[B] = Exp { () =>
    val (a,rs) = thunk()
    (f(a), rs)
  }

  def flatMap[B](f: A => Exp[B]): Exp[B] = Exp { () =>
    val (a,rsa) = thunk()
    val (b,rsb) = f(a).thunk()
    (b, rsa union rsb)
  }

  def run(): A = {
    val (a,_) = thunk()
    a
  }

}

object Exp {

  def unit[A](a: A): Exp[A] = Exp( () => (a,Set.empty[Cell]) )

  def map2[A,B,C](a: Exp[A], b: Exp[B])(f: (A,B) => C): Exp[C] =
      for {
      _a <- a
      _b <- b
    } yield f(_a, _b)

}
