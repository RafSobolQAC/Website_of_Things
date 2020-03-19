package services


import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json._
import collection._
import models.JsonFormats._
import models.{Thing, ThingWithID}
import play.api.libs.json.{JsObject, JsValue, Json}
import reactivemongo.api.Cursor
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global

class MongoServices @Inject()(
                               val reactiveMongoApi: ReactiveMongoApi
                             ) extends ReactiveMongoComponents {

  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("things"))

  def create(thing: Thing): Future[WriteResult] = {
    collection.flatMap(_.insert.one(thing))
  }

  def deleteThing(id: String) = {
    collection.flatMap(_.delete.one(
      Json.obj(
        {
          "_id" -> BSONObjectID.parse(id).getOrElse(throw new Exception("Wrong ID!"))
        }
      )))
  }
  def getThings(filter: JsObject) = {
    val cursor: Future[Cursor[ThingWithID]] = collection.map {
      _.find(filter)
        .cursor[ThingWithID]()
    }
    cursor.flatMap(
      _.collect[List](
        -1,
        Cursor.FailOnError[List[ThingWithID]]()
      ))
  }

  def updateThing(id: String, thing: Thing) = {
    collection.flatMap(_.update(false).one(
      Json.obj(
        {
          "_id" -> BSONObjectID.parse(id).get
        }
      ), thing
    ))
  }
}
