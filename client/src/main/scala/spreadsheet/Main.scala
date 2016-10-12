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
    val spreadSheetList = document.createElement("div")

    def openSpreadsheet(name: String): Unit = {
      val cellsElem = document.getElementById("cells")
      println(s"OPENING: $name")
      val connection = new SpreadsheetConnection(cellsElem, name)
    }

    val modal: Element = Modal(modalId, "Create a new spreadsheet or select an existing one") {
      val root = document.createElement("div")
      val listPanel1 = document.createElement("div")
      listPanel1.classList.add("panel")
      listPanel1.classList.add("panel-default")
      listPanel1 appendChild {
        val panelBody = document.createElement("div")
        panelBody.classList.add("panel-body")
        panelBody.appendChild(spreadSheetList)
        panelBody
      }
      root.appendChild(listPanel1)
      val listPanel2 = document.createElement("div")
      listPanel2.classList.add("panel")
      listPanel2.classList.add("panel-default")
      listPanel2.appendChild {
        val form = document.createElement("form")
        import org.scalajs.dom.raw.HTMLInputElement
        val input = document.createElement("input").asInstanceOf[HTMLInputElement]
        input.setAttribute("type","text")
        input.setAttribute("placeholder","Name")
        input.classList.add("form-control")
        form.appendChild {
          input
        }
        form.appendChild {
          import org.scalajs.dom.raw.HTMLButtonElement
          val button = document.createElement("button").asInstanceOf[HTMLButtonElement]
          button.setAttribute("type","submit")
          button.classList.add("btn")
          button.classList.add("btn-default")
          button.appendChild(document.createTextNode("Create new spreadsheet"))
          button.addEventListener("click", { _: Event =>
                                    jQuery(s"#$modalId").modal("hide")
                                    openSpreadsheet(input.value.replaceAll(" +", "-"))
                                  })
          button
        }
        form
      }
      root.appendChild(listPanel2)

      root
    }

    modalsElem.appendChild(modal)
    jQuery(modal).modal("show")

    HttpRequest(url).send().onComplete {
      case res:Success[SimpleHttpResponse] =>
        val spreadsheetNames = read[List[String]](res.get.body)
        println(s"spreadsheetNames = $spreadsheetNames")
        spreadsheetNames foreach { name =>
          val button = createButton(name, onClick = { () =>
                                      jQuery(modal).modal("hide")
                                      openSpreadsheet(name)
          })
          spreadSheetList.appendChild(button)
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
