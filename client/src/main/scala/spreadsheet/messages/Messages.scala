package spreadsheet.messages

import org.scalajs.dom._

object Messages {

  private def connectingMsgClasses =
    document
      .getElementById("connectingmessage")
      .classList

  private def maxReconnectionsExceededMsgClasses =
    document
      .getElementById("maxreconnections")
      .classList

  private def disconnectMessageClasses =
    document
      .getElementById("disconnectmessage")
      .classList

  def showDisconnectMsg() = disconnectMessageClasses.remove("hidden")

  def hideDisconnectMsg() = disconnectMessageClasses.add("hidden")

  def showMaxRetriesExceeded() = maxReconnectionsExceededMsgClasses.remove("hidden")

  def showConnectingMsg() = connectingMsgClasses.remove("hidden")

  def hideConnectingMsg() = connectingMsgClasses.add("hidden")

}
