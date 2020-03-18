package controllers

import javax.inject.{Inject, _}
import models.{Thing, ThingWithID}
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json._
import models.JsonFormats._
import play.api.libs.json.{JsValue, Json}
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.{JSONCollection, _}
import services.MongoServices

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class ThingController @Inject()(
                                 components: ControllerComponents,
                                 val reactiveMongoApi: ReactiveMongoApi,
                                 val mongoServices: MongoServices
                               ) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

  implicit def ec: ExecutionContext = components.executionContext

  def collection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("things"))

  var thingsList: List[ThingWithID] = List()

  def makeThings = {
    val cursor: Future[Cursor[ThingWithID]] = collection.map { el =>
      el.find(Json.obj())
        .cursor[ThingWithID]()
    }

    cursor.flatMap(el => {
      println(el + " is here!")
      el.collect[List](
        -1,
        Cursor.FailOnError[List[ThingWithID]]()
      )
    })
      .map { things =>
        println("I'm in thingslist!")
        thingsList = things
      }
  }

  def create(thing: Thing) = Action.async { implicit request: Request[AnyContent] =>
    mongoServices.create(thing).map(_ => {
      Await.result(makeThings, Duration.Inf)
      Ok(views.html.things(Thing.createThingForm, thingsList))
    })
  }

  def updateThing(id: String) = Action.async {implicit request: Request[AnyContent] =>
    mongoServices.updateThing(id).map(_ => {
      makeThings
      Ok(views.html.things(Thing.createThingForm, thingsList))
    })
  }



  def showThingForm = Action { implicit request: Request[AnyContent] =>
    Await.result(makeThings, Duration.Inf)
    Ok(views.html.things(Thing.createThingForm, thingsList))
  }

  def submitForm: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    Thing.createThingForm.bindFromRequest.fold({ formWithErrors =>
      println(formWithErrors)
      Future.successful(BadRequest(views.html.things(formWithErrors, thingsList)))
    }, { thing =>
      collection.flatMap(_.insert.one(thing)).map(_ => {
        Await.result(makeThings, Duration.Inf)
        Ok(views.html.things(Thing.createThingForm, thingsList))
      }
      )
    })
  }

  def deleteThing(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    mongoServices.deleteThing(id).map(_ => {
      Await.result(makeThings, Duration.Inf)
      Ok(views.html.things(Thing.createThingForm, thingsList))
    })
  }

  def getThings(filter: Option[(String, Json.JsValueWrapper)] = None): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    mongoServices.getThings(getOrNothing(filter)).map { things =>
      thingsList = things
      Ok(views.html.justThings(thingsList))
    }
  }


  def getOrNothing(filter: Option[(String, Json.JsValueWrapper)]) = {
    if (filter.isDefined) Json.obj(filter.get) else Json.obj()
  }

  def getThingsWithFilter(filtered: String, value: String): Action[AnyContent] = {
    getThings(Some((filtered, Json.toJsFieldJsValueWrapper(value))))
  }



}
