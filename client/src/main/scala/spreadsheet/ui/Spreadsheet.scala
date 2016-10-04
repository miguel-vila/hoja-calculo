package spreadsheet.ui

import spreadsheet._
import spreadsheet.frp.{Exp, Cell}
import spreadsheet.interpreter.{NumberValue, StringValue, Interpreter}
import spreadsheet.parsing.Parser
import Parser.{ Success, Failure , Error }

import org.scalajs.dom
import org.scalajs.dom.html
import dom.document
import org.scalajs.dom.raw.Element
import scala.scalajs.js.timers._

import scala.concurrent.duration._

import woot._

case class Spreadsheet(
  private var spreadsheet: SpreadSheetContent,
  broadcastOperation: SpreadSheetOp => Unit
) {

  val m = spreadsheet.content.size
  val n = spreadsheet.content(0).size
  val siteId = spreadsheet.siteId

  document.body.addEventListener("keydown", handleKeypresses _)

  def dir(keycode: Int): (Int,Int) = keycode match {
    case 37 => (0,-1)
    case 39 => (0,+1)
    case 38 => (-1,0)
    case 40 => (+1,0)
    case _  => (0, 0)
  }

  def isDir(keycode: Int): Boolean = 37 <= keycode && keycode <= 40

  private var cursorCell = Option.empty[SpreadsheetCell]

  val CURSOR_CLASS = "cursor"

  private def setCursorCell(cell: SpreadsheetCell) = {
    cursorCell.foreach(_.htmlElement.classList.remove(CURSOR_CLASS))
    cursorCell = Some(cell)
    cell.htmlElement.classList.add(CURSOR_CLASS)
    editCellInput.setPlaceholder( cell.expression )
  }

  private var connected = true

  def handleKeypresses(event: dom.KeyboardEvent) = if(connected) {
    //println(s"keycode = ${event.keyCode}")
    if(isDir(event.keyCode) && !editCellInput.isEditing) {
      val (dx,dy) = dir(event.keyCode)
      cursorCell.map { spreadsheetCell =>
        val CellId(row, column) = spreadsheetCell.cell.id
        val (x,y) = (row+dx,column+dy)
        val newCellOpt = for {
          row <- spreadsheetCells.lift(x)
          cell <- row.lift(y)
        } yield cell
        newCellOpt.foreach { cell =>
          setCursorCell(cell)
        }
      }
    } else if(!editCellInput.isEditing && event.keyCode == ENTER) {
      cursorCell.foreach(editCellInput.setCurrentCell)
    } else if(editCellInput.isEditing && (event.keyCode == ESCAPE || event.keyCode == ENTER) ) {
      val cell = editCellInput.currentCell.get
      editCellInput.removeCurrentCell()
      setCursorCell(cell)
    }
  }

  val ESCAPE = 27
  val ENTER = 13

  private val cells = Vector.tabulate(m,n) { (i,j) =>
    Cell( CellId(i,j), Exp.unit(StringValue("")) )
  }

  def resetSpreadsheetInfo(newSpreadsheet: SpreadSheetContent) = {
    spreadsheet = newSpreadsheet
    for {
      (row,i) <- spreadsheetCells.zipWithIndex
      (cell,j) <- row.zipWithIndex
    } {
      val wstring = spreadsheet.content(i)(j)
      cell.resetValue( wstring )
    }
    editCellInput.currentCell.foreach(editCellInput.setCurrentCell)
  }

  private val spreadsheetCells = for {
    (row,i) <- cells.zipWithIndex
  } yield for {
    (cell,j) <- row.zipWithIndex
  } yield {
    val wstring = spreadsheet.content(i)(j)
    val spcell = SpreadsheetCell(cell = cell, wstring = wstring)
    spcell.evaluate()
    spcell
  }

  private val editCellInput = new EditCellInput(None)

  val htmlElement = document.createElement("div")

  htmlElement.appendChild(editCellInput.htmlElement)

  spreadsheetCells(0)(0).htmlElement.click()

  val table = Table(
    m,
    n,
    columnText = j => CellId.columnChar(j).toString(),
    rowText = i => (i+1).toString(),
    tableElement = (i,j) => spreadsheetCells(i)(j).htmlElement
  )
  htmlElement.appendChild {
    val div = document.createElement("div")
    div.classList.add("table-container")
    div.appendChild(table)
    div
  }

  private case class EditCellInput(var currentCell: Option[SpreadsheetCell]) {

    val htmlElement = document.createElement("input").asInstanceOf[html.Input]
    htmlElement.`type` = "text"
    htmlElement.classList.add("form-control")
    htmlElement.placeholder = "Select a cell"
    htmlElement.readOnly = true

    htmlElement.addEventListener("input", onChange _)

    def setCurrentCell(cell: SpreadsheetCell) = {
      htmlElement.readOnly = false
      currentCell.foreach(_.htmlElement.classList.remove(CURSOR_CLASS))
      htmlElement.value = cell.expression
      currentCell = Some(cell)
      cell.htmlElement.classList.add(CURSOR_CLASS)
      htmlElement.focus()
    }

    def setPlaceholder(s: String) = {
      htmlElement.placeholder = s
    }

    def isEditing: Boolean = currentCell.isDefined

    def removeCurrentCell() = {
      currentCell.foreach(_.htmlElement.classList.remove(CURSOR_CLASS))
      htmlElement.value = ""
      htmlElement.readOnly = true
      currentCell = None
    }

    def onChange(event: dom.KeyboardEvent) = if(connected) {
      currentCell.foreach {
        _.processUserInput(htmlElement.value)
      }
    }

    def setText(text: String): Unit = {
      htmlElement.value = text
    }

    def disableEdition() = {
      htmlElement.readOnly = true
    }

    def enableEdition() = {
      htmlElement.readOnly = false
    }

  }

  private def getCell(cellId: CellId): Cell = {
    //println(s"getting cell ${cellId.row} ${cellId.column}")
    cells(cellId.row)(cellId.column)
  }

  private case class SpreadsheetCell(cell: Cell, var wstring: WString) {

    var expression = wstring.text

    def resetValue(_wstring: WString): Unit = {
      wstring = _wstring
      expression = wstring.text
    }

    val output = {
      val elem = document.createElement("input").asInstanceOf[html.Input]
      elem.`type` = "text"
      elem.readOnly = true
      elem.classList.add("cell")
      elem
    }

    output.addEventListener(
      "click",
      { _: dom.Event =>
        if(connected) {
          setCursorCell(this)
        }
      }
    )

    def htmlElement = output

    cell addListener { value =>
      output.value = value.toString()
      putValueChangedBorder()
    }

    def putValueChangedBorder() = {
      output.classList.add("value-changed")
      setTimeout(250 millis) {
        output.classList.remove("value-changed")
      }
    }

    def evaluate() = {
      Parser.parse(expression) match {
        case Success(ast,_) =>

          try {
            val newExp = Interpreter.evaluate(ast)(getCell)
            cell.set( newExp )
            removeErrorStyle()
          } catch {
            case t: Throwable =>
              println(s"Error: $t")
              t.printStackTrace()
              setErrorStyle()
              //output.value = "Evaluation error"
          }
        case Failure(msg,_) =>
          println(s"Failure = $msg")
          setErrorStyle()
          //output.value = "Parse error"
        case Error(msg,_) =>
          println(s"Error = $msg")
          setErrorStyle()
          //output.value = "Parse error"
      }
    }

    def setErrorStyle() = htmlElement.classList.add("error-in-cell")

    def removeErrorStyle() = htmlElement.classList.remove("error-in-cell")

    def processUserInput(newExpression: String) = {
      //println(s"old exp = $expression")
      if(expression != newExpression) {
        val (op, newWstr) = Utils.findOp(expression, newExpression) match {
          case Insert(char,pos) =>
            println(s"inserting $char at pos $pos for wstring = '${wstring.text}''")
            wstring.insert(char,pos)
          case Delete(pos)      =>
            println(s"deleting char at pos $pos for wstring = '${wstring.text}''}")
            wstring.delete(pos)
        }
        println(s"Previous wstring = ${wstring.text}")
        wstring = newWstr
        expression = newExpression
        println(s"Next wstring = ${wstring.text}")
        broadcastOperation(SpreadSheetOp(cell.id, op))

        evaluate()
      }
    }

    def integrateRemoteOperation(operation: Operation): Unit = {
      val (executedOps, newWstring) = wstring.integrate(operation)
      println(s"executedOps = ${executedOps.size}")
      wstring = newWstring
      expression = wstring.text
      if(editCellInput.currentCell == Some(this)) {
        editCellInput.setText(expression)
      }
      evaluate()
    }

    def setDisabledStyle() =
      htmlElement.classList.add("disabled")

    def removeDisabledStyle() =
      htmlElement.classList.remove("disabled")

  }

  def receiveRemoteOperation(cellOp: SpreadSheetOp): Unit = {
    val CellId(row,column) = cellOp.cellId
    spreadsheetCells(row)(column).integrateRemoteOperation(cellOp.op)
  }

  def disableEdition(): Unit = {
    println(s"Disabling edition")
    connected = false
    editCellInput.disableEdition()
    for {
      row <- spreadsheetCells
      cell <- row
    } cell.setDisabledStyle()
  }

  def enableEdition(): Unit = {
    println(s"Enabling edition")
    connected = true
    editCellInput.enableEdition()
    for {
      row <- spreadsheetCells
      cell <- row
    } cell.removeDisabledStyle()
  }

}
