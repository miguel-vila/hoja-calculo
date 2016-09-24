package co.mglvl.spreadsheet.ui

import co.mglvl.spreadsheet.frp.{Exp, Cell}
import co.mglvl.spreadsheet.interpreter.{LiteralValue, FloatValue, Interpreter}
import co.mglvl.spreadsheet.parsing.Parser
import org.scalajs.dom
import org.scalajs.dom.html
import Parser.{ Success, Failure , Error }
import org.scalajs.dom
import org.scalajs.dom.html
import dom.document
import org.scalajs.dom.raw.Element
import scala.scalajs.js.timers._
import scala.concurrent.duration._

case class Spreadsheet(m: Int, n: Int, root: Element) {

  def column(n: Int): Char = ('A'+n).toChar

  def id(i: Int, j: Int): String = s"${column(j)}$i"

  private val cells = Vector.tabulate(m,n) { (i,j) =>
    println(s"id = ${id(i,j)}")
    Cell( id(i,j), Exp.unit(FloatValue(0.0f): LiteralValue) )
  }

  // @TODO make this work for m > 9 and n > 26
  private def getCell(id: String): Cell[LiteralValue] = {
    println(s"id = $id")
    var column = (id(0) - 'A').toInt
    println(s"column = $column")
    var row = (id(1) - '0').toInt - 1
    println(s"row = $row")
    cells(row)(column)
  }

  private val spreadsheetCells = for {
    row <- cells
  } yield {
    for {
      cell <- row
    } yield SpreadsheetCell(cell)
  }

  private val editCellInput = new EditCellInput(None)

  root.appendChild(editCellInput.htmlElement)

  val table = document.createElement("table")
  table.classList.add("table-bordered")
  val thead = document.createElement("thead")
  table.appendChild(thead)
  val htr = document.createElement("tr")
  thead.appendChild(htr)
  val tbody = document.createElement("tbody")
  table.appendChild(tbody)
  htr.appendChild {
    val th = document.createElement("th")
    th
  }
  for { i <- (0 to m-1) } {
    htr.appendChild {
      val th = document.createElement("th")
      th.appendChild(document.createTextNode(column(i).toString()))
      th
    }
    val tr = document.createElement("tr")
    val th = document.createElement("th")
    th.setAttribute("scope", "row")
    th.appendChild(document.createTextNode( (i+1).toString) )
    tr.appendChild(th)
    for { j <- (0 to n-1) } {
      val td = document.createElement("td")
      td.appendChild( spreadsheetCells(i)(j).htmlElement )
      tr.appendChild(td)
    }
    tbody.appendChild(tr)
  }
  root.appendChild(table)

  private case class EditCellInput(var pointedCell: Option[SpreadsheetCell]) {

    val htmlElement = document.createElement("input").asInstanceOf[html.Input]
    htmlElement.`type` = "text"
    htmlElement.classList.add("form-control")

    htmlElement.addEventListener("change", onChange _)
    htmlElement.addEventListener("keyup", onChange _)
    htmlElement.addEventListener("keypress", onChange _)

    def setPointedCell(cell: SpreadsheetCell) = {
      println(s"prev = ${pointedCell.map(_.htmlElement.classList)}")
      pointedCell.foreach(_.htmlElement.classList.remove("editing-cell"))
      htmlElement.value = cell.expression
      pointedCell = Some(cell)
      cell.htmlElement.classList.add("editing-cell")
    }

    def onChange(event: dom.KeyboardEvent) = {
      pointedCell.foreach{
        _.changeExpression(htmlElement.value)
      }
    }

  }

  private case class SpreadsheetCell(cell: Cell[LiteralValue]) {

//    root.appendChild( document.createTextNode(s"$$${cell.id.toString} = ") )

    var expression = ""

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
      setTimeout(350 millis) {
        output.classList.remove("value-changed")
      }
    }

    def changeExpression(newExpression: String) = {
      if(newExpression.trim != "" && expression != newExpression) {
        Parser.parse(newExpression) match {
          case Success(ast,_) =>
            try {
              val newExp = Interpreter.evaluate(ast)(getCell)
              cell.set( newExp )
              expression = newExpression
            } catch {
              case t: Throwable =>
                println(s"Error: $t")
                t.printStackTrace()
                output.value = "Evaluation error"
                expression = ""
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


  }

}
