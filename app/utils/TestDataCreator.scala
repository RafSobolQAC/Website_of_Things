package utils

import controllers.ThingController
import javax.inject.Inject
import models.Thing
import services.MongoServices

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Random

object TestDataCreator {
  val tags: List[String] = List(
    "a",
    "b",
    "c",
    "d",
    "e",
    "f",
    "g",
    "h",
    "i",
    "j"
  )
  val home = System.getProperty("user.home")
  val names: List[String] = Source.fromFile(s"${home}/names.txt").getLines().toList
  val prices: List[BigDecimal] = List(
    0.99,
    1.50,
    2.50,
    3.50,
    4.50,
    4.99,
    5.00,
    5.50,
    6.00,
    15.00,
    29.99,
    39.99,
    49.99
  )
}
