package co.mglvl.spreadsheet.frp

case class Cell[A](
                  val id: Int,
                  private var code: Exp[A],
                  private var _value: Option[A],
                  var reads: Set[Cell[_]],
                  var observers: Set[Cell[_]]
  ) {

  override def equals(o: Any): Boolean = o match {
    case cell: Cell[A] => cell.id == id
    case _ => false
  }

  override def hashCode() = id.hashCode()

  def get(): Exp[A] = Exp { () =>
    _value match {
      case Some(a) => (a, Set(this))
      case None =>
        val (a, rds) = code()
        _value = Some(a)
        reads = rds
        rds foreach { rd =>
          rd.observers = rd.observers + this
        }
        (a, Set(this))
    }
  }

  def set(exp: Exp[A]): Unit = {
    invalidate()
    code = exp
  }
  private def invalidate(): Unit = {
    invalidateReadValues()
    invalidateObservedValue()
  }

  private def invalidateReadValues(): Unit = {
    reads.foreach(_.removeObserver(this))
    reads = Set.empty
  }

  private def invalidateObservedValue(): Unit = {
    _value = None
    observers.foreach(_.invalidateObservedValue())
  }

  private def removeObserver(cell: Cell[_]): Unit = {
    observers = observers - cell
  }

}

object Cell {

  private var id = 0

  private def newId(): Int = {
    val _id = id
    id += 1
    _id
  }

  def apply[A](exp: Exp[A]): Cell[A] =
    new Cell(
      id = newId(),
      code = exp,
      _value = None,
      reads = Set.empty,
      observers = Set.empty
    )

}