package co.mglvl.spreadsheet.frp

class Cell[A](
  val id: String,
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
    update()
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

  var listeners = List.empty[A => Unit]

  private def update(): Unit = {
    val newValue = get().run
    listeners.foreach(_(newValue))
    observers foreach (_.update())
  }

  def addListener(f: A => Unit): Unit = {
    listeners = f :: listeners
  }

}

object Cell {

  def apply[A](id: String, exp: Exp[A]): Cell[A] =
    new Cell(
      id = id,
      code = exp,
      _value = None,
      reads = Set.empty,
      observers = Set.empty
    )

}
