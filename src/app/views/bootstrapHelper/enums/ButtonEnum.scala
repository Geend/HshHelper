package views.bootstrapHelper.enums

object ButtonType extends Enumeration {
  val Button = Value("button")
  val Submit = Value("submit")
  val Type = Value("type")
}

object ButtonClass extends Enumeration {
  val Primary = Value("btn-primary")
  val Secondary = Value("btn-secondary")
  val Success = Value("btn-success")
  val Danger = Value("btn-danger")
  val Warning = Value("btn-warning")
  val Info = Value("btn-info")
  val Light = Value("btn-light")
  val Dark = Value("btn-dark")

  val PrimaryOutline = Value("btn-outline-primary")
  val SecondaryOutline = Value("btn-outline-secondary")
  val SuccessOutline = Value("btn-outline-success")
  val DangerOutline = Value("btn-outline-danger")
  val WarningOutline = Value("btn-outline-warning")
  val InfoOutline = Value("btn-outline-info")
  val LightOutline = Value("btn-outline-light")
  val DarkOutline = Value("btn-outline-dark")
}