package spreadsheet.ui

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom._

@js.native
trait JQueryStatic extends js.Object {

  def apply(selector: String): JQuery = js.native
  def apply(element: Element): JQuery = js.native

}

@js.native
trait JQuery extends js.Object {
  def modal(str: String): Unit = js.native
}
