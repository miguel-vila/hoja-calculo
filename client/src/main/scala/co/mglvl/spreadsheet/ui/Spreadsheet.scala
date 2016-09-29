package co.mglvl.spreadsheet.ui

import co.spreadsheet._
import co.mglvl.spreadsheet.frp.{Exp, Cell}
import co.mglvl.spreadsheet.interpreter.{FloatValue, Interpreter}
import co.mglvl.spreadsheet.parsing.Parser
import Parser.{ Success, Failure , Error }

import org.scalajs.dom
import org.scalajs.dom.html
import dom.document
import org.scalajs.dom.raw.Element
import scala.scalajs.js.timers._

import scala.concurrent.duration._

import woot._

case class Spreadsheet(spreadsheet: SpreadSheetContent, root: Element, broadcastOperation: SpreadSheetOp => Unit) {

  val m = spreadsheet.content.size
  val n = spreadsheet.content(0).size
  val siteId = spreadsheet.siteId

  private val cells = Vector.tabulate(m,n) { (i,j) =>
    val wstr = spreadsheet.content(i)(j)
    Cell( CellId(i,j), Exp.unit(0.0f) )
  }

  private val spreadsheetCells = for {
    (row,i) <- cells zipWithIndex
  } yield {
    for {
      (cell,j) <- row zipWithIndex
    } yield {
      val wstring = spreadsheet.content(i)(j)
      SpreadsheetCell(cell = cell, wstring = wstring)
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
      htmlElement.placeholder = ""
      htmlElement.readOnly = false
      pointedCell.foreach(_.htmlElement.classList.remove("editing-cell"))
      htmlElement.value = cell.expression
      pointedCell = Some(cell)
      cell.htmlElement.classList.add("editing-cell")
    }

    def onChange(event: dom.KeyboardEvent) = {
      pointedCell.foreach {
        _.changeExpression(htmlElement.value, broadcast = true)
      }
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
      root.appendChild(elem)
      elem
    }

    output.addEventListener("click", { _: dom.Event =>
                              editCellInput.setPointedCell(this)
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

    def changeExpression(newExpression: String, broadcast: Boolean) = {
      println(s"old exp = $expression")
      if(newExpression.trim != "" && expression != newExpression) {
        if(broadcast) {
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
          println(s"Next wstring = ${wstring.text}")
          broadcastOperation(SpreadSheetOp(cell.id, op))
        }
        expression = newExpression

        Parser.parse(newExpression) match {
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
    }

    def integrateRemoteOperation(operation: Operation): Unit = {
      val (executedOps, newWstring) = wstring.integrate(operation)
      println(s"executedOps = ${executedOps.size}")
      wstring = newWstring
      changeExpression( wstring.text, broadcast = false )
    }

  }

  def receiveRemoteOperation(cellOp: SpreadSheetOp): Unit = {
    val CellId(row,column) = cellOp.cellId
    spreadsheetCells(row)(column).integrateRemoteOperation(cellOp.op)
  }

}
