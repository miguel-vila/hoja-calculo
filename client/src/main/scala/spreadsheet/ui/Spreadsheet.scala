package spreadsheet.ui

import spreadsheet._
import spreadsheet.frp.{Exp, Cell}
import spreadsheet.interpreter.{FloatValue, StringValue, Interpreter}
import spreadsheet.parsing.Parser
import Parser.{ Success, Failure , Error }

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.MouseEvent
import dom.document
import org.scalajs.dom.raw.Element
import scala.scalajs.js.timers._

import scala.concurrent.duration._

import woot._

case class Spreadsheet(spreadsheet: SpreadSheetContent, root: Element, broadcastOperation: SpreadSheetOp => Unit) {

  val m = spreadsheet.content.size
  val n = spreadsheet.content(0).size
  val siteId = spreadsheet.siteId

  root.addEventListener("keydown", handleKeypresses _)

  def dir(keycode: Int): (Int,Int) = keycode match {
    case 37 => (0,-1)
    case 39 => (0,+1)
    case 38 => (-1,0)
    case 40 => (+1,0)
    case _  => (0, 0)
  }

  def handleKeypresses(event: dom.KeyboardEvent) = {
    val (dx,dy) = dir(event.keyCode)

    editCellInput.pointedCell.map { spreadsheetCell =>
      val CellId(row, column) = spreadsheetCell.cell.id
      val (x,y) = (row+dx,column+dy)
      println(s"${(x,y)}")
      //val event = new MouseEvent()
      //event.window = dom.window
      //event.bubbles = false
      //spreadsheetCells(x)(y).htmlElement.dispatchEvent(event)
      val newCellOpt = for {
        row <- spreadsheetCells.lift(x)
        cell <- row.lift(y)
      } yield cell
      newCellOpt.foreach { cell =>
        editCellInput.setPointedCell( cell )
      }
    }
  }

  private val cells = Vector.tabulate(m,n) { (i,j) =>
    val wstr = spreadsheet.content(i)(j)
    Cell( CellId(i,j), Exp.unit(StringValue("")) )
  }

  private val spreadsheetCells = for {
    (row,i) <- cells zipWithIndex
  } yield {
    for {
      (cell,j) <- row zipWithIndex
    } yield {
      val wstring = spreadsheet.content(i)(j)
      val spcell = SpreadsheetCell(cell = cell, wstring = wstring)
      spcell.evaluate()
      spcell
    }
  }

  private val editCellInput = new EditCellInput(None)

  root.appendChild(editCellInput.htmlElement)

  val table = Table(
    m,
    n,
    columnText = j => CellId.columnChar(j).toString(),
    rowText = i => (i+1).toString(),
    tableElement = (i,j) => spreadsheetCells(i)(j).htmlElement
  )
  root.appendChild(table)

  private case class EditCellInput(var pointedCell: Option[SpreadsheetCell]) {

    val htmlElement = document.createElement("input").asInstanceOf[html.Input]
    htmlElement.`type` = "text"
    htmlElement.classList.add("form-control")
    htmlElement.placeholder = "Select a cell"
    htmlElement.readOnly = true

    htmlElement.addEventListener("input", onChange _)

    def setPointedCell(cell: SpreadsheetCell) = {
      htmlElement.readOnly = false
      pointedCell.foreach(_.htmlElement.classList.remove("editing-cell"))
      htmlElement.value = cell.expression
      pointedCell = Some(cell)
      cell.htmlElement.classList.add("editing-cell")
    }

    def onChange(event: dom.KeyboardEvent) = {
      pointedCell.foreach {
        _.processUserInput(htmlElement.value)
      }
    }

    def setText(text: String): Unit = {
      htmlElement.value = text
    }

  }

  private def getCell(cellId: CellId): Cell = {
    cells(cellId.row)(cellId.column)
  }

  private case class SpreadsheetCell(cell: Cell, var wstring: WString) {

    var expression = wstring.text

    val output = {
      val elem = document.createElement("input").asInstanceOf[html.Input]
      elem.`type` = "text"
      elem.readOnly = true
      elem.classList.add("form-control")
      elem
    }

    output.addEventListener("click", { _: dom.Event =>
                              editCellInput.setPointedCell(this) } )

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
          } catch {
            case t: Throwable =>
              println(s"Error: $t")
              t.printStackTrace()
              output.value = "Evaluation error"
          }
        case Failure(msg,_) =>
          println(s"Failure = $msg")
          output.value = "Parse error"
        case Error(msg,_) =>
          println(s"Error = $msg")
          output.value = "Parse error"
      }
    }

    def processUserInput(newExpression: String) = {
      println(s"old exp = $expression")
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
      if(editCellInput.pointedCell == Some(this)) {
        editCellInput.setText(expression)
      }
      evaluate()
    }

  }

  def receiveRemoteOperation(cellOp: SpreadSheetOp): Unit = {
    val CellId(row,column) = cellOp.cellId
    spreadsheetCells(row)(column).integrateRemoteOperation(cellOp.op)
  }

}
