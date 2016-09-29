package co.mglvl.spreadsheet.frp

import co.spreadsheet.CellId

class Cell(
  val id: CellId,
  private var code: Exp[Float],
  private var _value: Option[Float],
  var reads: Set[Cell],
  var observers: Set[Cell]
  ) {

  override def equals(o: Any): Boolean = o match {
    case cell: Cell => cell.id == id
    case _ => false
  }

  override def hashCode() = id.hashCode()

  def get(): Exp[Float] = Exp { () =>
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

  def set(exp: Exp[Float]): Unit = {
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

  private def removeObserver(cell: Cell): Unit = {
    observers = observers - cell
  }

  var listeners = List.empty[Float => Unit]

  private def update(): Unit = {
    val newValue = get().run
    listeners.foreach(_(newValue))
    observers foreach (_.update())
  }

  def addListener(f: Float => Unit): Unit = {
    listeners = f :: listeners
  }

}

object Cell {

  def apply(id: CellId, exp: Exp[Float]): Cell =
    new Cell(
      id = id,
      code = exp,
      _value = None,
      reads = Set.empty,
      observers = Set.empty
    )

}
