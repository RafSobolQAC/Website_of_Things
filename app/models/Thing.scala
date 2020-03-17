package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.OFormat

case class Thing(
                  name: String,
                  price: BigDecimal,
                  tags: List[Tag]
                )

case class Tag(
                name: String
              )

object Thing {
  val createThingForm: Form[Thing] = Form(
    mapping(
      "name" -> nonEmptyText,
      "price" -> bigDecimal(7, 2),
      "tags" -> list(
        mapping(
          "name" -> text
        )(Tag.apply)(Tag.unapply)
      ))(Thing.apply)(Thing.unapply)

  )
}

object JsonFormats {
  import play.api.libs.json.{Json,_}
  implicit val tagFormat: OFormat[Tag] = Json.format[Tag]
  implicit val thingFormat: OFormat[Thing] = Json.format[Thing]

}
