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
                  tags: List[String]
                ) {
  override def toString: String = s"Thing: $name, costs $price, and it has: ${Thing.listToString(tags)}"
}
case class Id($oid: String)



case class ThingWithID(
                        _id: BSONObjectID,
                        name: String,
                        price: BigDecimal,
                        tags: List[String]
                      ) {
  override def toString: String = s"Thing: $name, costs $price, and it has: ${Thing.listToString(tags)}"

}
//case class ThingWithID(
//                        _id: String,
//                        name: String,
//                        price: BigDecimal,
//                        tags: List[String]
//                      ) {
//  def this(_id: Id, name: String, price: BigDecimal, tags: List[String]) {
//    this(_id.toString, name, price, tags)
//  }
//}

object ObjectIdFormatJsonMacro extends Format[BSONObjectID] {

  def writes(objectId: BSONObjectID): JsValue = JsString(objectId.toString())

  def reads(json: JsValue): JsResult[BSONObjectID] = json match {
    case JsString(x) => {
      val maybeOID: Try[BSONObjectID] = BSONObjectID.parse(x)
      if (maybeOID.isSuccess) JsSuccess(maybeOID.get) else {
        JsError("Expected ObjectId as JsString")
      }
    }
    case _ => JsError("Expected ObjectId as JsString")
  }
}

case class ThingWithObjectId(
                              _id: BSONObjectID,
                              name: String,
                              price: BigDecimal,
                              tags: List[String]
                            )


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
  implicit val idFormat: OFormat[Id] = Json.format[Id]
//  implicit val bsonFormat = ObjectIdFormatJsonMacro
  implicit val thingFormat: OFormat[Thing] = Json.format[Thing]
  implicit val thingWithIDFormat: OFormat[ThingWithID] = Json.format[ThingWithID]
  implicit val thingWithObjID: OFormat[ThingWithObjectId] = Json.format[ThingWithObjectId]
}
