package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.api.bson._
import scala.util.Try
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

case class Thing(
                  name: String,
                  price: BigDecimal,
                  var tags: List[String]
                ) {
  override def toString: String = s"Thing: $name, costs $price, and it has: ${Thing.listToString(tags)}"
}


case class ThingWithID(
                        _id: BSONObjectID,
                        name: String,
                        price: BigDecimal,
                        var tags: List[String]
                      ) {
  override def toString: String = s"Thing: $name, costs $price, and it has: ${Thing.listToString(tags)}"

}


object Thing {
  val createThingForm: Form[Thing] = Form(
    mapping(
      "name" -> nonEmptyText,
      "price" -> bigDecimal(7, 2),
      "tags" -> list(text)
    )(Thing.apply)(Thing.unapply)

  )

  def listToString[T](list: List[T]): String = {
    var sbuilder: StringBuilder = new StringBuilder
    list.foreach(tag => sbuilder ++= tag.toString + " ")
    sbuilder.toString()
  }

}

object JsonFormats {
  implicit val thingFormat: OFormat[Thing] = Json.format[Thing]
  implicit val thingWithIDFormat: OFormat[ThingWithID] = Json.format[ThingWithID]
}
