package spreadsheet

import scala.scalajs.js

package object ui {

  val jQuery: JQueryStatic = js.Dynamic.global.jQuery.asInstanceOf[JQueryStatic]

}
