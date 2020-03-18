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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class ThingController @Inject()(
                                 components: ControllerComponents,
                                 val reactiveMongoApi: ReactiveMongoApi

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
      println(thingsList)
    }
  }

  def create(thing: Thing) = Action.async { implicit request: Request[AnyContent] =>
    val futureResult = collection.flatMap(_.insert.one(thing))
    makeThings
    futureResult.map(_ => Ok(views.html.things(Thing.createThingForm, thingsList)))
  }

  def createFromJson: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Thing].map { thing =>
      collection.flatMap(_.insert.one(thing)).map { _ =>
        makeThings
        Ok("Thing created!")
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Json format!")))
  }


  def deleteThingFromForm = Action.async(parse.json) { implicit request: Request[JsValue] =>
    request.body.validate[Thing].map { thing =>
      collection.flatMap(_.delete.one(thing)).map { _ =>
        Await.result(makeThings, Duration.Inf)

        Ok("Deleted!")
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Json!")))
  }

  def showThingForm = Action { implicit request: Request[AnyContent] =>
    Await.result(makeThings, Duration.Inf)
    Ok(views.html.things(Thing.createThingForm, thingsList))
  }

  def submitForm = Action.async { implicit request: Request[AnyContent] =>
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

  def deleteThing(id: String) = Action.async { implicit request: Request[AnyContent] =>
    collection.flatMap(_.delete.one(
      Json.obj({
        "_id" -> BSONObjectID.parse(id).get
      })

    )).map(_ => {
      Await.result(makeThings, Duration.Inf)
      Ok(views.html.things(Thing.createThingForm, thingsList))
    })
  }

  def getThings(filter: Option[(String, Json.JsValueWrapper)] = None): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val cursor: Future[Cursor[ThingWithID]] = collection.map {
      _.find(getOrNothing(filter))
        .cursor[ThingWithID]()
    }
    cursor.flatMap(
      _.collect[List](
        -1,
        Cursor.FailOnError[List[ThingWithID]]()
      )
    ).map { things =>
      thingsList = things
      Ok(views.html.things(Thing.createThingForm, thingsList))
    }
  }


  def getOrNothing(filter: Option[(String, Json.JsValueWrapper)]) = {
    if (filter.isDefined) Json.obj(filter.get) else Json.obj()
  }

  def getThingsWithFilter(filtered: String, value: String): Action[AnyContent] = {
    getThings(Some((filtered, Json.toJsFieldJsValueWrapper(value))))
  }


}
