package co.mglvl.spreadsheet.ui

import co.mglvl.spreadsheet.frp.{Exp, Cell}
import co.mglvl.spreadsheet.interpreter.Interpreter
import co.mglvl.spreadsheet.parsing.Parser
import org.scalajs.dom
import org.scalajs.dom.html
import Parser.{ Success, Failure , Error }
import org.scalajs.dom
import org.scalajs.dom.html
import dom.document
import org.scalajs.dom.raw.Element

case class Spreadsheet(n: Int, root: Element) {

  private val cells = Vector.fill(n)( Cell( Exp.unit(0.0f) ) )

  private val spreadsheetCells = for {
    cell <- cells
  } yield SpreadsheetCell(cell)

  spreadsheetCells.foreach { spreadsheetCell =>
    root.appendChild( spreadsheetCell.htmlElement )
  }

  private case class SpreadsheetCell(cell: Cell[Float]) {
    private var lastValue: Option[String] = None

    private val root = document.createElement("div")

    def htmlElement = root
    root.appendChild( document.createTextNode(s"$$${cell.id.toString} = ") )

    val input: html.Input = {
      val elem = document.createElement("input").asInstanceOf[html.Input]
      elem.`type` = "text"
      root.appendChild(elem)
      elem.addEventListener("change", onChange _)
      elem.addEventListener("keyup", onChange _)
      elem.addEventListener("keypress", onChange _)
      elem
    }

    val output = {
      val elem = document.createElement("input").asInstanceOf[html.Input]
      elem.`type` = "text"
      elem.readOnly = true
      root.appendChild(elem)
      elem
    }

    def setValue(): Unit = {
      output.value = cell.get().run().toString
      cell.observers.foreach { obs =>
        spreadsheetCells( obs.id ).setValue()
      }
    }

    val validKeyCodes: Set[Int] = Set(
      8,//backspace
      9,//tab
      16,//shift
      18,//alt
      20,//capslock
      187,//+ y *
      55,// /
      189,//-
      16, //$
      56, //(
      57 //)
    ) ++ Range(48,58) //digits

    def onChange(event: dom.KeyboardEvent) = {
      if(lastValue.forall(_ != input.value)) {
        Parser.parse(input.value) match {
          case Success(ast,_) =>
            val newExp = Interpreter.evaluate(ast)(cells)
            cell.set( newExp )
            setValue()
            lastValue = Some(input.value)
          case Failure(msg,_) =>
            output.value = msg
          case Error(msg,_) =>
            output.value = msg
        }
      }
    }


  }

}
