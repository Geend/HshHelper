package views.bootstrapHelper

import views.bootstrapHelper.enums.InputType

class Input(val typ: InputType.Value,
            val label: String,
            val placeholder: String = "",
            val help: String = "") {

}

object Input {
  def apply(typ: InputType.Value, label: String, placeholder: String = "", help: String = ""):
  Input = new Input(typ, label, placeholder, help)
}