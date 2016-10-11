package spreadsheet

import org.scalajs.dom
import dom._

object Modal {

  def apply(id: String, title: String)(innerContent: Element): Element = {
    val root = document.createElement("div")
    root.setAttribute("id", id)
    root.setAttribute("role", "dialog")
    root.classList.add("modal")
    root.classList.add("fade")
    root.appendChild {
      val modalDialog = document.createElement("div")
      modalDialog.classList.add("modal-dialog")
      modalDialog.appendChild {
        val content = document.createElement("div")
        content.classList.add("modal-content")
        content.appendChild {
          val header = document.createElement("div")
          header.classList.add("modal-header")
          header.appendChild {
            val titleElem = document.createElement("h4")
            titleElem.classList.add("modal-title")
            titleElem.appendChild(document.createTextNode(title))
            titleElem
          }
          header
        }
        content.appendChild {
          val body = document.createElement("div")
          body.classList.add("modal-body")
          body.appendChild(innerContent)
          body
        }
        content
      }
      modalDialog
    }
    root
  }

}
