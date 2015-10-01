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

case class Spreadsheet(n: Int, root: Element) {

  private val cells = Vector.fill(n)( Cell( Exp.unit(FloatValue(0.0f): LiteralValue) ) )

  private val spreadsheetCells = for {
    cell <- cells
  } yield SpreadsheetCell(cell)

  spreadsheetCells.foreach { spreadsheetCell =>
    root.appendChild( spreadsheetCell.htmlElement )
  }

  private case class SpreadsheetCell(cell: Cell[LiteralValue]) {
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

    def onChange(event: dom.KeyboardEvent) = {
      if(input.value.trim != "" && lastValue != Some(input.value)) {
        Parser.parse(input.value) match {
          case Success(ast,_) =>
            try {
              val newExp = Interpreter.evaluate(ast)(cells)
              cell.set( newExp )
              setValue()
              lastValue = Some(input.value)
            } catch {
              case t: Throwable =>
                println(s"Error: $t")
                t.printStackTrace()
                output.value = "Evaluation error"
                lastValue = None
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
