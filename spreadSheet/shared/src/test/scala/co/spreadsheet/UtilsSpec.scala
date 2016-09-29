package co.spreadsheet

import org.scalatest._

class UtilsSpec extends FlatSpec with Matchers {

  "Utils" should "compute the correct operation given two strings" in {

    Utils.findOp("", "a") should equal (Insert('a',0))
    Utils.findOp("a", "ab") should equal (Insert('b',1))
    Utils.findOp("abcde", "abcdef") should equal (Insert('f',5))

    Utils.findOp("abcde", "abcxde") should equal (Insert('x',3))
    Utils.findOp("abcde", "xabcde") should equal (Insert('x',0))

    Utils.findOp("abcde", "abcd") should equal (Delete(4))
    Utils.findOp("abcde", "abde") should equal (Delete(2))
    Utils.findOp("abcde", "bcde") should equal (Delete(0))

  }

}
