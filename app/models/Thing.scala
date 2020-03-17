package models

import play.api.libs.json.OFormat

case class Thing(
                  name: String,
                  price: BigDecimal,

                  tags: List[Tag]
                )

case class Tag(
                name: String
              )


//object JsonFormats {
//  import play.api.libs.json.Json
//
//  implicit val feedFormat: OFormat[Tag] = Json.format[Tag]
//  implicit val userFormat: OFormat[Thing] = Json.format[Thing]
//}
