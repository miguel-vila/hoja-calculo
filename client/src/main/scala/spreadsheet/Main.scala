package spreadsheet

import scala.scalajs.js
import org.scalajs.dom
import dom._
import spreadsheet.ui.jQuery

import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global
import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse

import upickle.legacy._

object Main extends js.JSApp {

  def main(): Unit = {
    val url = s"${dom.window.location.protocol}//${dom.window.location.host}/spreadsheet-names"

    val modalId ="spreadsheet-names"
    val modalsElem = document.getElementById("modals")
    val content = document.createElement("div")
    val modal = Modal(modalId, "Create new spreadsheet or select an existing one") {
      content
    }
    modalsElem.appendChild(modal)
    jQuery(modal).modal("show")

    HttpRequest(url).send().onComplete {
      case res:Success[SimpleHttpResponse] =>
        val spreadsheetNames = read[List[String]](res.get.body)
        spreadsheetNames foreach { name =>
          val button = createButton(name, onClick = { () =>
            jQuery(modal).modal("hide")
            val cellsElem = document.getElementById("cells")
            val connection = new SpreadsheetConnection(cellsElem, name)
          })
          content.appendChild(button)
        }
      case e: Failure[SimpleHttpResponse] => println("Fuuuuck!")

    }
    /*
    val cellsElem = document.getElementById("cells")
    val connection = new SpreadsheetConnection(cellsElem)
     */
  }

  import org.scalajs.dom.raw.HTMLButtonElement
  def createButton(text: String, onClick: () => Unit): HTMLButtonElement = {
    val button = document.createElement("button").asInstanceOf[HTMLButtonElement]
    button.classList.add("btn")
    button.classList.add("btn-primary")
    button.setAttribute("type", "button")
    button.addEventListener("click", { _: Event => onClick() } )
    button.appendChild(document.createTextNode(text))
    button
  }

}
