package spreadsheet

object Utils {

  def findOp(before: String, after: String): Op = {
    if(before.length() > after.length()) {
      require(before.length() - after.length() == 1, s"expected string difference of one got $before -> $after")
      var i = 0
      while(i < after.length()) {
        if(before(i) != after(i)) {
          return Delete(i)
        }
        i += 1
      }
      return Delete(i)
    } else {
      println(s"before = $before")
      println(s"after = $after")
      require(after.length() - before.length() == 1, s"expected string difference of one got $before -> $after")
      var i = 0
      while(i < before.length()) {
        if(before(i) != after(i)) {
          return Insert(after(i), i)
        }
        i += 1
      }
      return Insert(after(i),i)
    }
  }

}

trait Op
case class Insert(char: Char, index: Int) extends Op
case class Delete(index: Int) extends Op
