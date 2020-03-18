package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,_}

case class Thing(
                  name: String,
                  price: BigDecimal,
                  tags: List[Tag]
                ) {
  override def toString: String = s"Thing: $name, costs $price, and it has: ${Thing.listToString(tags)}"
}

case class Tag(
                name: String
              ) {
  override def toString: String = s"tag: $name"

}

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
  def listToString[T](list: List[T]): String = {
    var sbuilder: StringBuilder = new StringBuilder
    list.foreach(tag => sbuilder ++= tag.toString + " ")
    sbuilder.toString()
  }

}

object JsonFormats {
  implicit val tagFormat: OFormat[Tag] = Json.format[Tag]
  implicit val thingFormat: OFormat[Thing] = Json.format[Thing]

}
