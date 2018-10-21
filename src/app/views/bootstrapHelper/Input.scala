package views.bootstrapHelper

import views.bootstrapHelper.enums.InputType

class Input(val typ: InputType.Value,
            val label: String = "",
            val value: Either[String, Seq[(String, String)]] = Left(""),
            val placeholder: String = "",
            val help: String = "") {

}

object Input {
  def apply(typ: InputType.Value,
            label: String = "",
            value: Either[String, Seq[(String, String)]] = Left(""),
            placeholder: String = "",
            help: String = ""): Input =
    new Input(typ, label, value, placeholder, help)
}